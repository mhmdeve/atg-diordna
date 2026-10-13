// Android port of the iOS touch-control gesture mapping.
// The game's logical buttons/actions match src/touch.rs. The iOS UIKit drawing code is
// intentionally isolated to Apple targets; Android can layer its own native overlay later.
use crate::input;
use std::collections::{HashMap, HashSet};
use std::sync::{LazyLock, Mutex};
use std::time::{Duration, Instant};
use winit::event::{Touch, TouchPhase};

const MIN_HOLD: Duration = Duration::from_millis(120);
const LOOK_GAIN: f64 = 3.0;
const STICK_RADIUS: f64 = 58.0;
const SPRINT_REACH: f64 = 1.35;
static TOUCH_SENS: LazyLock<f64> =
    LazyLock::new(|| std::env::var("MUGUET_TOUCH_SENS").ok().and_then(|v| v.parse().ok()).unwrap_or(1.0));

#[derive(Clone, Copy)]
enum Action { Key(&'static str), Mouse(u8), Aim, Nav, Horn }

#[derive(Clone, Copy)]
struct Button { x: f64, y: f64, radius: f64, foot: Action, car: Action }

const BUTTONS: &[Button] = &[
    Button{x:62.,y:68.,radius:40.,foot:Action::Mouse(1),car:Action::Key("KeyW")},
    Button{x:158.,y:46.,radius:31.,foot:Action::Aim,car:Action::Key("KeyS")},
    Button{x:58.,y:168.,radius:28.,foot:Action::Key("Space"),car:Action::Key("Space")},
    Button{x:140.,y:132.,radius:26.,foot:Action::Key("KeyF"),car:Action::Key("KeyF")},
    Button{x:222.,y:98.,radius:24.,foot:Action::Key("KeyR"),car:Action::Horn},
    Button{x:30.,y:30.,radius:22.,foot:Action::Key("Tab"),car:Action::Key("Tab")},
    Button{x:84.,y:30.,radius:22.,foot:Action::Key("KeyQ"),car:Action::Key("KeyQ")},
    Button{x:138.,y:30.,radius:22.,foot:Action::Key("ControlLeft"),car:Action::Key("KeyC")},
    Button{x:192.,y:30.,radius:22.,foot:Action::Key("KeyV"),car:Action::Key("KeyV")},
    Button{x:30.,y:30.,radius:22.,foot:Action::Key("Escape"),car:Action::Key("Escape")},
    Button{x:84.,y:30.,radius:22.,foot:Action::Key("ArrowUp"),car:Action::Key("ArrowUp")},
    Button{x:138.,y:30.,radius:22.,foot:Action::Nav,car:Action::Nav},
    Button{x:192.,y:30.,radius:22.,foot:Action::Key("AltLeft"),car:Action::Key("AltLeft")},
];

#[derive(Clone, Copy)]
enum Grip {
    Stick { origin: (f64,f64), at: (f64,f64) },
    Look { last: (f64,f64) },
    Button { idx: usize, start: (f64,f64), last: (f64,f64) },
}
struct State {
    size: (f64,f64),
    scale: f64,
    grips: HashMap<u64,Grip>,
    held: HashSet<&'static str>,
    since: HashMap<&'static str,Instant>,
    mouse: u8,
    mouse_since: [Instant;2],
    aim: bool,
    nav: bool,
    in_car: bool,
}
impl Default for State {
    fn default() -> Self {
        Self { size:(1280.,720.),scale:1.,grips:HashMap::new(),held:HashSet::new(),since:HashMap::new(),mouse:0,
            mouse_since:[Instant::now();2],aim:false,nav:false,in_car:false }
    }
}
static STATE: LazyLock<Mutex<State>> = LazyLock::new(|| Mutex::new(State::default()));

pub fn touch(t: &Touch) {
    let mut s=STATE.lock().unwrap();
    s.size = (1280.0, 720.0);
    let p=(t.location.x/s.scale,t.location.y/s.scale);
    match t.phase {
        TouchPhase::Started => {
            let is_left = p.0 < s.size.0*0.4 && p.1 > 70.;
            let in_button = BUTTONS.iter().enumerate().find(|(_,b)| {
                let (cx,cy)=button_center(b,&s);
                let r=b.radius+8.;
                (p.0-cx).powi(2)+(p.1-cy).powi(2)<=r*r
            }).map(|(i,_)|i);
            if let Some(idx)=in_button {
                s.grips.insert(t.id,Grip::Button{idx,start:p,last:p});
                match BUTTONS[idx].foot { Action::Aim if !s.in_car=>s.aim=!s.aim, Action::Nav=>{},_=>{} }
            } else if is_left && !s.grips.values().any(|g|matches!(g,Grip::Stick{..})) {
                s.grips.insert(t.id,Grip::Stick{origin:p,at:p});
            } else { s.grips.insert(t.id,Grip::Look{last:p}); }
        }
        TouchPhase::Moved => match s.grips.get_mut(&t.id) {
            Some(Grip::Stick{at,..})=>*at=p,
            Some(Grip::Look{last})|Some(Grip::Button{last,..})=>{
                let dx=p.0-last.0; let dy=p.1-last.1; *last=p;
                let speed=(dx*dx+dy*dy).sqrt();
                input::mouse_delta_f(dx*LOOK_GAIN**TOUCH_SENS*(1.+(speed/12.).min(1.5)),dy*LOOK_GAIN**TOUCH_SENS*(1.+(speed/12.).min(1.5)));
            }
            _=>{}
        },
        TouchPhase::Ended|TouchPhase::Cancelled=>{s.grips.remove(&t.id);}
    }
    apply(&mut s);
}

fn button_center(b:&Button,s:&State)->(f64,f64) {
    let right=s.size.0-b.x;
    let bottom=s.size.1-b.y;
    if b.y==30. {
        if b.x<=192. { return (s.size.0-b.x,b.y); }
    }
    (right,bottom)
}
fn apply(s:&mut State) {
    let mut keys:HashSet<&'static str>=HashSet::new();
    let mut mouse=if s.aim&&!s.in_car {2u8}else{0};
    let mut stick=None; let mut horn=false;
    for g in s.grips.values() {
        match g {
            Grip::Stick{origin,at}=>stick=Some((*origin,*at)),
            Grip::Button{idx,..}=>match if s.in_car {BUTTONS[*idx].car}else{BUTTONS[*idx].foot} {
                Action::Key(k)=>{keys.insert(k);},
                Action::Mouse(b)=>mouse|=b,
                Action::Horn=>horn=true,
                Action::Nav=>s.nav=!s.nav,
                Action::Aim|Action::Nav=>{},
            },
            Grip::Look{..}=>{}
        }
    }
    if let Some((o,at))=stick {
        let mut dx=at.0-o.0;let mut dy=at.1-o.1;let len=(dx*dx+dy*dy).sqrt();
        let k=if len>STICK_RADIUS{STICK_RADIUS/len}else{1.};dx*=k;dy*=k;
        let nx=dx/STICK_RADIUS;let ny=dy/STICK_RADIUS;
        if nx< -0.35 {keys.insert("KeyA");} if nx>0.35{keys.insert("KeyD");}
        if s.in_car {if ny< -0.6{keys.insert("Numpad8");} if ny>0.6{keys.insert("Numpad2");}}
        else {if ny< -0.35{keys.insert("KeyW");}if ny>0.35{keys.insert("KeyS");}}
        if !s.in_car&&len>STICK_RADIUS*SPRINT_REACH{keys.insert("ShiftLeft");}
    }
    crate::pad::HORN_TOUCH.store(horn, std::sync::atomic::Ordering::Relaxed);
    let now=Instant::now();
    let mut next=keys.clone();
    for k in s.held.clone() {
        if keys.contains(k){continue}
        if s.since.get(k).is_some_and(|t|now.duration_since(*t)<MIN_HOLD){next.insert(k);}
        else {input::key(k,false,false,None,false);s.since.remove(k);}
    }
    for k in next.difference(&s.held){crate::mp::key_pressed(k);input::key(k,true,false,None,false);s.since.insert(k,now);}
    s.held=next;
    for (bit,dom) in [(1u8,0usize),(2u8,2usize)].into_iter() {
        if (s.mouse^mouse)&bit!=0 { input::button(dom,mouse&bit!=0); if mouse&bit!=0{s.mouse_since[if bit==1{0}else{1}]=now;} }
    }
    s.mouse=mouse;
}

pub fn refresh()->bool { false }
pub fn release() {
    let mut s=STATE.lock().unwrap();
    s.grips.clear();s.aim=false;apply(&mut s);
}
