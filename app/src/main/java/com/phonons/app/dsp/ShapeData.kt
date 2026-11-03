package com.phonons.app.dsp

/**
 * Hand-written library entries, one per line: name | group | search words | description | hz:db ...
 *
 * Kept as text rather than Kotlin objects because a list this long would exceed the JVM's limit on
 * the size of one method. A unit test checks every line.
 */
internal object ShapeData {
    val blocks: List<Pair<ShapeCategory, String>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        listOf(
            ShapeCategory.MUSIC to music1,
            ShapeCategory.MUSIC to music2,
            ShapeCategory.INSTRUMENT to instruments,
            ShapeCategory.MEDIA to media,
            ShapeCategory.SITUATION to situations,
            ShapeCategory.FIX to fixes,
            ShapeCategory.GEAR to gear,
            ShapeCategory.CHARACTER to character,
        )
    }

    private const val music1 = """
Pop | Pop | chart radio mainstream | Punchy low end and clear, forward vocals. | 50:3 120:2.5 400:-1 2500:1.5 6000:1.5 12000:2.5
Modern pop | Pop | chart streaming | Tight sub bass with a bright, polished top. | 40:3.5 90:2.5 300:-1.5 1000:0 3500:1.5 12000:3.5
Dance pop | Pop | club chart | Solid kick and sparkle for upbeat tracks. | 50:4 100:3 350:-1.5 2000:0.5 8000:2 14000:3
Synth pop | Pop | 80s electropop | Rounded bass with shiny synths on top. | 60:3 150:1.5 500:-1 2000:1 6000:2 12000:3
Indie pop | Pop | alternative bedroom | Light and airy with intimate vocals. | 80:1.5 300:-1 1500:0.5 3500:1.5 10000:2
K-pop | Pop | kpop korean idol | Dense, bright mixes with a firm low end. | 50:3 100:2 400:-1.5 2500:1 6000:2 13000:3
J-pop | Pop | jpop japanese anime | Clear vocals and lively highs, less sub bass. | 60:1.5 200:0.5 500:-1 2500:1.5 6000:2 13000:2.5
Ballad | Pop | slow love song emotional | Warm body with a close, present voice. | 80:1.5 200:1.5 500:-0.5 2500:2 5000:1 12000:1
Soft pop | Pop | easy listening mellow | Relaxed top end with a gentle lift in warmth. | 80:1.5 250:1 1000:0 4000:-0.5 12000:-1.5
Teen pop | Pop | bright upbeat | Bright, bouncy and vocal-led. | 60:2 200:0 800:0 3000:2 8000:2.5 14000:2.5
Rock | Rock & metal | guitar band | Weighty drums and guitars with bite. | 60:3 120:2 400:-1.5 1200:0.5 3000:2.5 8000:2 14000:1.5
Classic rock | Rock & metal | 70s vintage guitar | Warm and midrange-rich, without modern brightness. | 70:2 200:1.5 800:1 2500:1.5 6000:0 14000:-1
Hard rock | Rock & metal | loud riffs | Thick riffs and a hard snare crack. | 60:3.5 120:2 450:-2 1500:1 3500:3 8000:1.5
Punk | Rock & metal | fast raw guitars | Raw midrange drive with less boom. | 80:1 200:0 800:1 2500:3 5000:2 12000:0
Grunge | Rock & metal | 90s alternative | Heavy low mids and a rough guitar edge. | 70:2 200:2 500:0 1500:1 3000:2 10000:-0.5
Alternative rock | Rock & metal | alt indie | Balanced, with extra guitar presence. | 60:1.5 150:1 500:-1 2000:1 4000:2 12000:1
Indie rock | Rock & metal | alternative jangly | Light low end and jangly guitars. | 80:1 300:-1 1000:0.5 3000:2 7000:1.5 13000:1
Post-rock | Rock & metal | ambient guitars instrumental | Wide, slow builds with open highs. | 50:2 150:0.5 500:-1 2000:0 6000:1.5 14000:3
Shoegaze | Rock & metal | dream noise wall guitars | Tames the wall of guitars and lifts vocals out of it. | 60:1.5 300:-1 1000:-1 2500:1.5 5000:-0.5 12000:2
Progressive rock | Rock & metal | prog complex | Detailed and even, so every part stays audible. | 60:1 200:0 600:-0.5 2000:1 5000:1 14000:2
Heavy metal | Rock & metal | guitars double kick | Scooped mids with fast kick and guitar edge. | 60:4 100:2.5 500:-3 1500:-0.5 3500:3 8000:2.5
Thrash metal | Rock & metal | fast riffs | Tighter low end so fast palm mutes stay clear. | 50:1 90:3 200:-0.5 500:-2.5 3000:3 6000:3 12000:1
Death metal | Rock & metal | growl blast beats | Separates low growls from dense guitars. | 50:2.5 100:3 300:-1 700:-3 2500:2.5 5000:3 10000:1
Black metal | Rock & metal | raw tremolo | Adds weight to thin mixes and softens fizz. | 60:4.5 150:3 500:0 2000:0 6000:-1.5 12000:-3
Djent | Rock & metal | prog metal tight | Very tight bass and hard attack in the upper mids. | 40:-1 80:3 160:2 400:-3 1200:-1 3000:3 6000:2
Doom metal | Rock & metal | stoner sludge slow | Huge low end, with fizz kept down. | 40:5 90:4 200:2 800:-1 3000:0.5 10000:-2
Power metal | Rock & metal | melodic symphonic | Clear vocals over bright, fast guitars. | 60:2.5 150:1 500:-2 2500:2 6000:2.5 14000:2
Metalcore | Rock & metal | breakdown screams | Heavy breakdowns with a clear screamed vocal. | 50:3.5 100:3 400:-2.5 1000:-0.5 3000:3 7000:2
Emo | Rock & metal | pop punk | Forward vocals and bright guitars. | 70:1.5 300:-1 1000:0.5 3000:2.5 7000:1.5 13000:1
Hip-hop | Hip-hop & R&B | rap hiphop | Deep bass with a clear vocal on top. | 40:5 80:4 180:1 500:-1.5 2500:1.5 8000:1 14000:1.5
Boom bap | Hip-hop & R&B | 90s rap samples | Round kick and a dusty, warm top. | 60:4 120:3 300:0 1000:0 3000:1 8000:-0.5 14000:-1
Trap | Hip-hop & R&B | 808 rap hi hats | Sub-heavy 808s with crisp hi-hats. | 30:6 60:5.5 130:1.5 400:-2 2000:0 8000:2.5 14000:3.5
Drill | Hip-hop & R&B | uk rap sliding 808 | Sliding low bass with a dry, up-front vocal. | 35:5.5 70:4.5 150:1 500:-2 2500:2 7000:1.5 13000:2
Conscious rap | Hip-hop & R&B | lyrics wordplay hiphop | Lets the words lead, with the beat just behind. | 60:2.5 150:0.5 500:-1 2500:3 5000:1.5 12000:0.5
Old school hip-hop | Hip-hop & R&B | 80s rap classic | Lean bass and a bright drum machine. | 60:2.5 150:1 500:0 2000:1 5000:1.5 12000:1
Lo-fi hip-hop | Hip-hop & R&B | study beats chill | Soft highs and warm, rounded bass. | 60:3 150:2 500:0 2000:-0.5 6000:-2 14000:-4
Phonk | Hip-hop & R&B | drift cowbell memphis | Heavy sub bass with a sharp cowbell edge. | 35:6 70:5 150:1.5 500:-2 3000:1.5 8000:1.5 14000:0
R&B | Hip-hop & R&B | rnb rhythm blues | Smooth low end and silky vocals. | 50:3.5 110:2.5 350:-1 1500:0.5 3500:1.5 12000:2
Neo soul | Hip-hop & R&B | rnb jazzy | Warm bass, soft keys and an intimate voice. | 60:2.5 180:1.5 500:0 2000:0.5 5000:0.5 13000:0.5
Alt R&B | Hip-hop & R&B | rnb moody atmospheric | Dark, deep and spacious, with airy vocals. | 45:4 100:2.5 400:-1.5 1500:0 4000:0.5 14000:2.5
Soul | Hip-hop & R&B | motown vintage | Warm mids and rich, natural vocals. | 80:2 200:1.5 700:0.5 2500:1.5 6000:0 14000:-0.5
Funk | Hip-hop & R&B | groove slap bass | Bouncy bass and snappy guitar. | 60:3 120:2.5 350:-1 1000:0 2500:2 7000:1.5 13000:1
Disco | Hip-hop & R&B | 70s dance strings | Four-on-the-floor kick with shimmering strings. | 55:3.5 110:2 400:-1 2000:0.5 7000:2 14000:2.5
Gospel | Hip-hop & R&B | choir church | Full choir warmth with a clear lead vocal. | 80:2 200:1.5 600:0 2500:2 6000:1 13000:1.5
"""

    private const val music2 = """
EDM | Electronic | festival dance big room | Big kick and bright leads. | 40:4.5 80:4 200:0 500:-2 2500:0.5 8000:2.5 14000:3.5
House | Electronic | deep dance club | Steady kick with room left for vocals. | 45:3.5 100:3 300:-1 1000:0 3000:1 9000:1.5 14000:2
Deep house | Electronic | chill dance | Warm, rolling bass with soft highs. | 45:4 100:3 300:0 1000:-0.5 4000:0 12000:0.5
Tech house | Electronic | club groove | Tight kick and crisp percussion. | 50:3.5 100:2.5 350:-2 1500:0 6000:2 13000:2.5
Progressive house | Electronic | melodic trance | Long builds with wide, bright synths. | 45:3 100:2 400:-1.5 2000:0.5 8000:2 14000:3
Techno | Electronic | berlin warehouse | Driving kick and dark, hypnotic mids. | 40:4.5 80:4 200:0.5 600:-1.5 3000:0.5 9000:1.5 14000:1
Minimal techno | Electronic | sparse hypnotic | Clean sub with small details left clear. | 40:3 80:2.5 250:-0.5 800:-1 4000:1 12000:2
Trance | Electronic | uplifting psy | Pumping low end with soaring leads. | 45:3.5 90:3 300:-1.5 1500:0.5 6000:2.5 14000:3.5
Psytrance | Electronic | goa psychedelic | Hard rolling bass and piercing detail. | 40:4 90:4 200:0 700:-2 4000:1.5 10000:3 15000:2
Drum and bass | Electronic | dnb jungle liquid | Deep sub with a fast, sharp snare. | 35:5 70:4 180:0 500:-2 2500:1.5 7000:2.5 13000:2.5
Liquid drum and bass | Electronic | dnb smooth soulful | Smooth sub with soft, soulful highs. | 40:4 80:3 250:0 800:-0.5 3000:1 8000:1 14000:1
Jungle | Electronic | breakbeat dnb 90s | Booming bass with chopped drums to the front. | 40:4.5 80:3.5 250:0 800:0 3000:2 7000:2 13000:0.5
Dubstep | Electronic | wobble bass growl | Massive sub with a midrange growl. | 35:6 70:5 160:1 450:-2 1500:1 5000:1.5 13000:2.5
Future bass | Electronic | melodic supersaw | Wide supersaws with a soft sub below. | 45:3.5 100:2 350:-1 2000:1 7000:2.5 14000:3
Hardstyle | Electronic | hardcore kick | Distorted kick with bright, intense leads. | 50:5 100:4.5 250:0 700:-2.5 3000:1 8000:3 14000:2.5
Garage | Electronic | uk 2 step speed | Bouncy bass with crisp, shuffled hats. | 45:3.5 100:2.5 350:-1 1500:0.5 6000:2 13000:2.5
Electro | Electronic | synth robotic | Punchy drum machine and edgy synths. | 50:3.5 110:2.5 400:-1 1500:1 4000:2 11000:2
Synthwave | Electronic | retrowave outrun 80s | Round bass, wide pads and a soft glow. | 60:3 150:1.5 500:-1 2000:0.5 6000:1 12000:1.5
Vaporwave | Electronic | slow retro aesthetic | Dreamy, hazy and slightly dark. | 60:2.5 200:1.5 800:0.5 3000:-1 8000:-2.5 14000:-4
Hyperpop | Electronic | glitch pc music | Keeps extreme highs in check and adds some body. | 50:2.5 150:1.5 500:0 2500:0.5 6000:-1 12000:-1.5
Glitch | Electronic | idm experimental | Sharp clicks and detail with a clean low end. | 50:2 150:0 500:-1 2500:0.5 8000:2.5 15000:3
IDM | Electronic | braindance experimental | Neutral with extra fine detail. | 45:1.5 150:0 600:-0.5 2500:0.5 8000:1.5 15000:2
Ambient | Electronic | atmospheric drone | Soft and spacious, with nothing sharp. | 50:2 200:0.5 800:-0.5 3000:-1 8000:0 14000:1
Downtempo | Electronic | chillout trip hop | Relaxed bass and soft, smooth highs. | 50:3 120:2 400:-0.5 1500:0 5000:-0.5 13000:0
Trip-hop | Electronic | triphop 90s bristol | Dusty drums with a dark, close vocal. | 55:3.5 120:2.5 400:0 2000:0.5 6000:-1 13000:-2
Reggaeton | Electronic | latin dembow perreo | Hard dembow kick with a bright vocal. | 45:4.5 90:3.5 250:0 600:-2 2500:1.5 8000:1.5 14000:2
Jazz | Jazz & blues | swing quartet acoustic | Natural warmth and open cymbals. | 60:1.5 200:1 600:-0.5 2000:0 6000:0.5 14000:1.5
Smooth jazz | Jazz & blues | sax lounge | Soft, polished and easy on the ear. | 70:2 200:1 700:0 3000:0 8000:-0.5 14000:0
Bebop | Jazz & blues | fast small group | Clear horns and walking bass from older recordings. | 80:2 200:1 700:0.5 2500:1.5 6000:1 12000:0
Big band | Jazz & blues | swing brass orchestra | Bright brass over a warm rhythm section. | 70:1.5 200:0.5 800:0 2500:1.5 6000:1.5 13000:1
Fusion | Jazz & blues | electric jazz rock | Tighter bass and more midrange clarity. | 60:2 180:0.5 600:-0.5 2500:1.5 6000:1 13000:1.5
Vocal jazz | Jazz & blues | crooner standards singer | Close, warm voice with soft brushes. | 80:1 200:1 500:0 2500:1.5 6000:0 13000:0.5
Bossa nova | Jazz & blues | brazil latin guitar | Gentle nylon guitar and a soft voice. | 80:0.5 200:1 700:0 2500:1 7000:0 14000:0.5
Blues | Jazz & blues | delta electric guitar | Warm mids and singing guitar. | 80:1.5 200:1.5 800:1 2500:1.5 6000:0 14000:-1
Delta blues | Jazz & blues | old acoustic 1930s | Adds some body to thin old recordings. | 100:3 250:2 800:0.5 2500:1 5000:-1 10000:-3
Classical | Classical | orchestra symphony | Almost untouched, with a slight lift for air. | 60:1 200:0.5 800:0 3000:0 8000:0.5 14000:1.5
Symphony | Classical | orchestra large romantic | Weight in the basses and an open string tone. | 50:2.5 150:1 500:-0.5 2500:0 7000:0.5 14000:1.5
Chamber music | Classical | string quartet small | Light, intimate and detailed. | 80:0.5 250:0.5 800:0 3000:0.5 8000:1 14000:1
Baroque | Classical | harpsichord bach | Crisp articulation for period instruments. | 100:0.5 300:0 1000:0 3000:1 7000:1.5 14000:1
Opera | Classical | soprano tenor aria | Clear voices over the orchestra. | 60:0.5 250:0 800:0.5 2800:2.5 5000:1 13000:0.5
Choral | Classical | choir sacred | Blended voices with a sense of space. | 80:0.5 200:1 600:0 2500:1 6000:0.5 14000:1.5
Solo piano | Classical | keys recital | Full lower octaves and a clean, soft attack. | 60:1.5 200:1 700:-0.5 2500:0.5 7000:0.5 14000:0.5
Film score | Classical | soundtrack cinematic orchestra | Big low brass and wide, detailed strings. | 45:3 120:1.5 500:-1 2000:0 7000:1 14000:2
Minimalism | Classical | contemporary repeating | Neutral, with repeating details kept clear. | 60:0.5 300:-0.5 1000:0 3000:0.5 8000:1 14000:1
Country | Folk & country | nashville twang | Clear vocals and bright acoustic guitar. | 80:1.5 200:0.5 600:-0.5 2500:2 6000:1.5 13000:1.5
Classic country | Folk & country | honky tonk steel old | Warm and honest, with a clear voice. | 90:1.5 250:1 800:0.5 2500:1.5 6000:0 13000:-0.5
Bluegrass | Folk & country | banjo mandolin fiddle | Quick, bright picking with natural body. | 100:0.5 300:0 1000:0.5 3000:2 7000:2 13000:1
Americana | Folk & country | roots singer songwriter | Earthy guitars and a close voice. | 80:1.5 220:1.5 700:0 2500:1.5 6000:0.5 13000:0
Folk | Folk & country | acoustic singer songwriter | Natural guitars and a warm, close voice. | 90:1 220:1 700:-0.5 2500:1.5 7000:1 14000:1
Indie folk | Folk & country | acoustic bedroom gentle | Soft and woody with a touch of air. | 90:1 250:1 800:-0.5 3000:0.5 8000:0.5 14000:1.5
Celtic | Folk & country | irish fiddle whistle | Bright fiddles and whistles with a light low end. | 90:0.5 300:-0.5 1000:0 3000:1.5 7000:2 13000:1.5
Acoustic | Folk & country | unplugged guitar | Keeps instruments sounding natural and woody. | 80:1 200:1 600:-0.5 2500:0.5 7000:1 14000:1.5
Reggae | World | jamaica ska roots | Round, deep bass and an airy skank guitar. | 50:4 100:3.5 250:1 800:-1.5 3000:0.5 8000:0.5 14000:0
Dub | World | echo jamaica | Very deep bass and dark, spacious echoes. | 45:5.5 90:4.5 220:1 800:-2 3000:-0.5 8000:-0.5 14000:-1
Ska | World | offbeat horns | Bright horns and a choppy guitar. | 70:1.5 200:0 700:0 2500:2 6000:1.5 13000:1
Afrobeats | World | nigeria amapiano african | Warm kick and bright percussion. | 50:3.5 100:3 300:-0.5 1000:0 3000:1.5 8000:1.5 14000:2
Amapiano | World | log drum south africa | Deep log drums with soft, airy keys. | 40:5 80:4 180:1 500:-1.5 2500:0.5 8000:1 14000:2
Latin pop | World | spanish tropical | Warm percussion and a forward vocal. | 60:2.5 150:1.5 500:-1 2500:1.5 7000:1.5 13000:2
Salsa | World | latin brass cuba | Bright horns over a tight rhythm section. | 70:1.5 200:0.5 700:0 2500:1.5 6500:2 13000:1.5
Bachata | World | latin guitar dominican | Delicate guitars and intimate vocals. | 80:1.5 220:0.5 800:-0.5 3000:1.5 7000:1.5 13000:1
Samba | World | brazil percussion | Lively percussion with a warm surdo drum. | 60:2.5 150:1 500:-0.5 2500:1 7000:2 13000:2
Flamenco | World | spanish guitar | Sharp guitar attack and clear handclaps. | 90:0.5 250:0.5 800:0 3000:2 7000:1.5 13000:1
Bollywood | World | hindi indian film | Forward vocals with bright, busy backing. | 60:2.5 150:1 500:-1 2500:2 6000:1.5 13000:1.5
Indian classical | World | sitar tabla raga | Rich tabla and shimmering strings. | 70:1.5 200:1 700:0 2500:0.5 7000:1 14000:2
Arabic | World | oud middle east | Warm oud with clear, ornamented vocals. | 80:1 220:1 700:0.5 2500:1.5 6000:0.5 13000:0.5
Mandopop | World | chinese cantopop c-pop | Smooth, vocal-led and gently bright. | 70:1.5 200:0.5 600:-0.5 2800:1.5 7000:1 13000:2
Fado | World | portuguese voice guitar | An intimate voice with a soft guitar behind it. | 100:0.5 250:1 800:0.5 2800:2 6000:0 13000:0
Polka | World | accordion dance | Bright accordion with less boom. | 100:0 300:-0.5 1000:1 3000:1.5 7000:1 13000:0
Music from the 50s | Eras | oldies mono rock and roll vintage | Adds some body and air to narrow early recordings. | 100:2.5 300:1 1000:0 3000:0.5 6000:0 12000:1.5
Music from the 60s | Eras | oldies motown beatles vintage | Warmer low end and a bit more top end. | 80:2.5 250:1 800:0 3000:0.5 7000:1 13000:1.5
Music from the 70s | Eras | vintage analog | Keeps analog warmth and adds a little clarity. | 70:2 220:1 800:0 3000:0.5 7000:0.5 14000:0.5
Music from the 80s | Eras | gated drums synth | Tames bright 80s digital sheen and adds weight. | 60:3 180:1.5 700:0 3000:-0.5 7000:-1 13000:-0.5
Music from the 90s | Eras | cd era | Modest bass and a slightly fuller top. | 50:2 130:1 500:-0.5 2500:0.5 7000:0.5 13000:1
Music from the 2000s | Eras | loudness war compressed | Softens dense, loud masters. | 50:1.5 150:0.5 700:0 3000:-1 6500:-1 13000:0
Modern remaster | Eras | loud bright reissue | Softens the brightness common in loud reissues. | 50:1 200:0.5 1000:0 3500:-1 7000:-1.5 14000:-1
"""

    private const val instruments = """
Lead vocals | Vocals | singer voice clarity | Brings singers forward from the band. | 100:-1.5 300:-1 1000:0.5 2800:3 5500:1.5 12000:0
Female vocals | Vocals | soprano woman singer | Clarity and air higher up the range. | 120:-1.5 400:-0.5 1200:0.5 3500:2.5 8000:1.5 13000:1.5
Male vocals | Vocals | baritone man singer | Chest warmth with added definition. | 100:0.5 220:1.5 500:-0.5 2300:2.5 5000:1 12000:0
Rap vocals | Vocals | mc bars words | Keeps words sharp over heavy beats. | 60:1 200:-1 700:0 2500:3 5500:2 12000:0
Breathy vocals | Vocals | intimate whisper air | Adds air and closeness to soft singers. | 150:-1 500:-0.5 2000:0.5 6000:1 12000:3
Harmony vocals | Vocals | backing choir stacks | Opens up stacks of voices. | 120:-1 400:-1 1200:0 3000:1.5 7000:1.5 13000:2
Acoustic guitar | Guitar | strum fingerpick | Body plus string detail. | 100:0 220:1.5 500:-1 2500:1 6000:2 13000:1.5
Electric guitar | Guitar | amp distortion riff | More guitar edge and midrange bite. | 100:-0.5 300:-1 900:1 2500:3 5000:1.5 10000:-0.5
Clean guitar | Guitar | jazz chorus jangle | Chime and sparkle for clean tones. | 120:0 400:-1 1500:0.5 3500:1.5 8000:2 13000:1
Lead guitar | Guitar | solo shred | Lifts solos above the rhythm section. | 150:-1 500:0 1200:2.5 2800:3 5500:0.5 11000:-0.5
Classical guitar | Guitar | nylon spanish | Warm nylon tone without string squeak. | 100:0.5 220:1.5 700:0 2500:1 6000:-0.5 13000:0
Bass guitar | Bass | electric fingers low | Round bass notes that are easy to follow. | 40:2 80:3 180:2.5 500:0 900:1 2500:0.5 8000:0
Slap bass | Bass | funk pop | Low thump plus string snap. | 40:2.5 90:3 250:-1 900:0 2500:2 5000:2 10000:0
Upright bass | Bass | double jazz acoustic | Woody tone and audible finger work. | 50:1.5 110:2.5 250:1.5 700:0.5 2000:1 6000:0
808 bass | Bass | sub trap | Pure low extension. | 28:6 55:5.5 110:2 220:0 500:-1
Sub bass | Bass | rumble low end | Only the deepest notes. | 25:6 45:5.5 80:2 140:0
Mid bass punch | Bass | kick impact body | Impact you can feel, without deep rumble. | 40:0 80:3 130:4.5 220:1.5 400:0
Kick drum | Drums | punch beater | Low thump plus beater click. | 50:4 90:3 250:-1.5 800:-0.5 3000:2.5 6000:0.5
Snare drum | Drums | crack backbeat | Snare body and crack. | 100:0 200:2.5 500:-1 1500:0 3000:1.5 5500:2.5 10000:0.5
Cymbals | Drums | hi hat ride crash | Shimmer and detail on top. | 1000:0 3500:0 8000:2.5 12000:3.5 16000:3
Drum kit | Drums | full band acoustic | Whole kit punch and openness. | 60:3 120:1.5 400:-2 1500:0 4000:1.5 10000:2
Percussion | Drums | congas shaker hand | Crisp attack for hand percussion. | 100:0.5 300:-0.5 1000:0 3500:1.5 7500:2.5 13000:1.5
Grand piano | Keys | concert classical | Rich lows and a clear attack. | 60:2 180:1 600:-0.5 2500:1 6000:0.5 13000:1
Upright piano | Keys | parlour honky | Softens a boxy piano tone. | 100:1 350:-1.5 1000:0 2800:1 6000:0.5 13000:0.5
Electric piano | Keys | rhodes wurlitzer | Warm bell-like tone. | 90:1 220:1 700:0 2000:1 5000:0.5 12000:-0.5
Organ | Keys | hammond church | Deep pedals and a warm midrange. | 40:3 100:2 400:0 1200:0.5 3500:0.5 10000:-0.5
Synthesizer | Keys | pads leads analog | Fat lows and fizzy harmonics. | 50:2.5 150:1 500:-1 2000:0.5 6000:1.5 13000:2.5
Violin | Strings | fiddle solo | Sweet tone without a scratchy edge. | 200:-0.5 500:0.5 1500:1 3500:0.5 7000:-1 13000:1
Cello | Strings | solo warm | Rich low strings and bow texture. | 70:1.5 180:2 400:1 1000:0 3000:1 8000:0.5
String section | Strings | orchestra ensemble | Silky and wide. | 100:0.5 350:-0.5 1000:0 3000:0 7500:1.5 14000:2.5
Harp | Strings | plucked glissando | Soft, glassy plucks. | 150:0.5 500:-0.5 1500:0 4000:1 8000:1.5 14000:2
Mandolin and banjo | Strings | bluegrass picked | Bright attack without getting thin. | 150:0.5 500:0 1500:0.5 3500:2 7000:1.5 13000:0
Ukulele | Strings | hawaii strum | Warmth for a small instrument. | 150:1.5 350:1 1000:0 3000:0.5 7000:1 13000:0.5
Trumpet | Brass & wind | horn jazz | Brassy bite and presence. | 150:-0.5 500:-0.5 1500:1.5 3500:2 7000:0.5 13000:0
Trombone and tuba | Brass & wind | low brass | Weight for the lower brass. | 60:2.5 150:2 400:1 1200:0.5 3500:0.5 9000:-0.5
Saxophone | Brass & wind | sax jazz alto tenor | Reedy warmth with some bite. | 120:0 300:1 900:1 2500:1.5 6000:0 12000:-0.5
Flute and clarinet | Brass & wind | woodwind | Breath and airy tone. | 200:-0.5 700:0 2000:1 5000:1 9000:1.5 14000:1
"""

    private const val media = """
Podcast | Speech | talk show interview voice | Clear talking, with rumble and boom removed. | 60:-8 120:-4 250:-0.5 2500:3.5 5500:1.5 10000:0 16000:-2
Podcast on a noisy commute | Speech | talk train bus voice | Pushes voices above low-frequency noise. | 60:-9 150:-5 400:-1 1500:1.5 3000:5 6000:2.5 12000:0
Audiobook | Speech | narrator reading story | Comfortable narrator tone for long sessions. | 60:-6 150:-2.5 400:0 2000:1.5 3500:2 7500:-1 14000:-3
Sleepy audiobook | Speech | night bed narrator | Soft, dark and free of sharp consonants. | 60:-5 150:-1.5 500:0.5 1800:1 3500:0 7000:-3 13000:-6
News | Speech | radio broadcast anchor | Crisp speech with a broadcast sound. | 70:-7 170:-2 500:0 2500:3 6000:1.5 13000:-1
Lecture | Speech | classroom talk video course | Makes poor room recordings easier to follow. | 100:-8 220:-4 500:-1 1500:1 3000:4 6000:1 11000:-3
Phone recording | Speech | voice memo call | Reduces rumble and hiss on rough recordings. | 90:-8 200:-3 500:0 1700:2 3200:3.5 6500:-1 11000:-5
Deep male voice | Speech | talk low narrator | Removes chest boom from low voices. | 70:-5 140:-4 250:-2 600:0 2500:2.5 6000:1 13000:0
Light female voice | Speech | talk high narrator | Softens sharp S sounds and adds body. | 80:-4 200:0.5 500:0.5 2200:2 4500:1 7500:-2.5 13000:-1
Sibilant talker | Speech | ess harsh s sounds | Softens S sounds while keeping words clear. | 100:-3 400:0 2000:1.5 3800:1.5 6500:-4 9000:-3 14000:-1
Foreign film speech | Speech | subtitles dialogue language | Makes unfamiliar speech a little easier to catch. | 80:-5 200:-2 800:0 2500:4 5000:2.5 10000:0
Movie | Film & TV | cinema film surround | Weight for effects and clear dialogue. | 35:4 70:3.5 200:0 500:-1 2500:2 7000:1 14000:1.5
Action movie | Film & TV | explosions blockbuster | More rumble with speech kept audible. | 30:6 60:5 150:1 500:-1.5 2500:2.5 7000:1.5 14000:2
Dialogue boost | Film & TV | hear voices mumbling | Makes quiet film dialogue easier to hear. | 60:-3 200:-2 500:0 1800:2 3000:4.5 6000:2 12000:0
Night-time movie | Film & TV | late quiet neighbours | Less rumble, more speech. | 40:-5 100:-3 300:-0.5 1500:1 3000:3 7000:0.5 13000:-1
Horror | Film & TV | tension drones scary | Deep drones and sharper tiny details. | 30:5 60:3.5 200:0 800:-1 3000:1 8000:2 14000:2
Animation | Film & TV | cartoon anime | Bright voices and lively effects. | 50:2 150:0.5 500:-0.5 2500:2 7000:2 13000:2
Documentary | Film & TV | narration nature | Clear narration with room for atmosphere. | 50:0.5 150:-1 500:0 2500:2.5 7000:1 14000:1
TV series | Film & TV | show drama streaming | Balanced for mixed speech and score. | 50:1.5 150:0.5 500:-0.5 2500:2 7000:0.5 14000:0.5
Music videos | Film & TV | youtube concerts | Fuller bass and clearer voices on online clips. | 50:2.5 130:1.5 400:-1 2500:1.5 7000:1 13000:1.5
Shooter game | Games | fps footsteps competitive | Lifts footsteps and reloads above explosions. | 40:-3 100:-2 300:-1 1000:0 3000:2 5000:4 9000:3 14000:1
Footstep finder | Games | fps competitive sound whoring | Strong emphasis on small high-frequency cues. | 40:-6 100:-5 300:-2 1000:0 3000:3 5500:6 9000:4 14000:0
Battle royale | Games | fps open world gunfire | Clearer direction cues with some bass kept. | 40:0 100:0 300:-1 1000:0 3000:2 6000:3 10000:2 14000:1
Racing game | Games | engine cars | Fuller engines with clearer tyres. | 40:4 80:3 200:0.5 700:-0.5 2500:1 7000:1.5 13000:1
Role-playing game | Games | rpg open world immersive | Big score and clear character voices. | 40:3 90:2 300:-0.5 800:-0.5 2500:1.5 7000:1 14000:2
Horror game | Games | tension footsteps creaks | Deep unease and small sounds nearby. | 35:4 70:2.5 250:-0.5 1000:0 3500:2 7500:3 14000:1
Mobile game | Games | phone casual | Clears up small, compressed game sound. | 60:1 200:0 500:-1 2500:1.5 7000:1 13000:0.5
Voice chat | Games | discord teammates mic | Makes voice chat easier to understand. | 80:-7 180:-3 400:0 1500:1.5 3000:4 6000:0 11000:-4
Video call | Calls | zoom meeting teams work | Cleans up thin, compressed call audio. | 90:-7 200:-2.5 500:0.5 1800:2 3200:3 7000:-1 12000:-4
Radio stream | Calls | internet low bitrate fm | Softens the hiss in low-quality streams. | 60:1 200:0.5 700:0 3000:1 7000:-2 12000:-4
"""

    private const val situations = """
Train commute | Travel | subway metro rail | Compensates for low rumble that masks music. | 40:3 90:4 200:2 500:0 2000:1 5000:2 12000:1
Bus ride | Travel | engine commute | Lifts low mids and vocals over engine drone. | 50:2 110:3.5 250:2.5 700:0.5 2500:2 7000:1 13000:0
Plane | Travel | flight cabin drone | Fills in what cabin drone hides. | 40:2 90:4.5 200:3.5 500:1 2500:2.5 6000:2 13000:1
Car with road noise | Travel | driving highway | Lifts the lows and presence covered by road noise. | 40:4 90:4 200:1.5 600:-0.5 2500:2 7000:2 13000:2
City walk | Travel | street traffic awareness | Keeps music clear over traffic. | 50:1.5 120:2 300:0.5 900:0 2800:2.5 7000:2 13000:1
Walk in the park | Travel | outdoor quiet nature | Light and open for quiet surroundings. | 60:1 200:0 700:-0.5 2500:0.5 7000:1 14000:2
Cycling | Travel | wind bike outdoor | Cuts through wind noise. | 80:-1 200:0 700:0.5 2500:3 6000:2 12000:0
Gym | Exercise | workout lifting weights | Heavy kick and an aggressive top. | 40:5 80:4.5 180:1 500:-2 2500:1 7000:2.5 14000:2.5
Running | Exercise | jog cardio pace | Punchy rhythm that's easy to follow. | 50:3 100:3 300:-1 1000:0 3000:1.5 8000:2 14000:1.5
Yoga | Exercise | stretch calm meditation | Soft, slow and calm. | 60:1.5 200:1 800:0 2500:-0.5 7000:-1 14000:-0.5
HIIT | Exercise | interval intense | The strongest punch for short efforts. | 40:6 80:5 200:1 600:-2.5 3000:2 8000:3 14000:3
Office | Daily | work background hvac | Light background sound with nothing sharp. | 60:-1 200:0 700:0 2500:0.5 6000:-0.5 13000:-1
Open-plan office | Daily | chatter work typing | Forward mids to hold up against chatter. | 60:0 200:1 800:1 2500:2 6000:0 13000:-1
Study | Daily | focus concentration reading | Soft and unobtrusive, so it stays in the background. | 60:1 200:0.5 800:0 2500:-1 6000:-1.5 14000:-1.5
Deep focus | Daily | concentration low distraction | Removes sharp sounds that grab attention. | 70:1 300:0.5 1000:0 3000:-2 6000:-3 14000:-4
Cooking | Daily | kitchen chores fan | Lifts voices and rhythm over kitchen noise. | 80:1 200:0.5 700:0 2500:2.5 7000:1.5 13000:0.5
Cleaning | Daily | chores vacuum | Punchy and bright over loud appliances. | 50:3 110:3 300:0 1000:0 3000:3 7000:3 13000:2
Party | Daily | friends speaker dance | Big and energetic for groups. | 40:5 90:4 250:0 700:-1.5 3000:1 8000:3 14000:3
Relaxing | Daily | chill unwind evening | Smooth and warm, with no edge. | 60:2 180:1.5 700:0 2500:-1 7000:-1.5 14000:-1
Morning | Daily | wake up coffee | Light, clear and easy to wake up to. | 70:0.5 250:-0.5 900:0 2500:1 7000:1.5 14000:2
Late night | Night | quiet low volume bed | Keeps detail at low volume without getting tiring. | 50:2.5 150:1 500:0 2500:0.5 7000:-0.5 14000:-1
Low volume listening | Night | quiet equal loudness | Lifts bass and air that ears miss at low volume. | 35:6 80:4.5 200:1.5 600:0 2500:0 8000:1.5 14000:3.5
Very low volume | Night | whisper quiet equal loudness | Stronger low-volume compensation. | 35:9 80:6.5 200:2.5 600:0 2500:0 8000:2.5 14000:5
Falling asleep | Night | bed sleep calm | Dark and soft, with almost no top end. | 60:1.5 200:1.5 800:0 2000:-2 5000:-5 12000:-8
Sleep sounds | Night | rain white noise | Softens hissy rain and noise tracks. | 60:2 200:1 800:0 2500:-1.5 6000:-4 14000:-7
Tinnitus-friendly | Night | ringing soft treble | Gentle treble reduction. Not a medical treatment. | 1000:0 3000:-1 6000:-2.5 10000:-3.5 14000:-4
Long sessions | Comfort | fatigue all day | Eases the region that tends to tire ears. | 80:0.5 300:0 1000:0 3000:-1 6500:-2 13000:-1
Quiet room | Comfort | home silent critical | Clean and detailed when nothing masks the music. | 50:0.5 300:-0.5 1000:0 3000:0 8000:0.5 14000:1
Noise cancelling on | Comfort | anc earbuds quiet | A light lift for detail with the outside world gone. | 50:0.5 150:0 700:-0.5 3000:0.5 8000:1 14000:1.5
Transparency mode on | Comfort | ambient aware earbuds | Makes up for the thinner bass of transparency mode. | 40:3 90:3 200:1.5 700:0 2500:0.5 8000:0 14000:0
"""

    private const val fixes = """
Too muddy | Low end | muddy unclear cloudy | Clears the build-up that blurs bass and low mids. | 100:0 200:-2 300:-3 500:-1.5 900:0
Too muddy, strong | Low end | muddy unclear cloudy | A bigger cut for very cloudy mixes. | 80:0 180:-3 300:-5 500:-3 1000:0
Too boomy | Low end | boomy bloated one note bass | Tames bass that hangs around. | 40:-1 80:-3 140:-3.5 250:-1 500:0
Too boomy, strong | Low end | boomy bloated one note bass | For bass that overwhelms everything. | 40:-3 80:-5.5 140:-5 250:-2 500:0
Rumble | Low end | subsonic thump rumble noise | Rolls off the very lowest notes. | 25:-9 40:-6 70:-2 120:0
Weak bass | Low end | thin no bass lacks | Adds fullness where bass feels missing. | 40:3.5 80:3.5 160:2.5 300:0.5 500:0
Weak bass, strong | Low end | thin no bass lacks | For earbuds that seal poorly or sound small. | 40:6 80:6 160:4 300:1 600:0
No punch | Low end | kick impact soft bass flat | Adds impact where kick drums land. | 50:0.5 90:3 130:3.5 220:1 400:-0.5 700:0
No sub bass | Low end | deep rumble missing | Extends the lowest octave only. | 25:5 45:4.5 80:1.5 130:0
Too thin | Body | weak lean bright no body | Adds body to the low mids. | 80:1 180:2.5 300:3 600:1 1200:0
Too thin, strong | Body | weak lean bright no body | A bigger warm lift for lean sound. | 80:2 180:4 300:4.5 600:2 1200:0
Too boxy | Body | cardboard closed in | Removes the "inside a box" sound. | 200:0 350:-2 500:-3 800:-1.5 1300:0
Too boxy, strong | Body | cardboard closed in | A deeper cut for very closed-in sound. | 180:0 350:-3.5 500:-5 800:-2.5 1400:0
Too hollow | Body | scooped distant missing mids | Restores mids that seem to be missing. | 250:0 500:1.5 1000:3 2000:2 4000:0
Too hollow, strong | Body | scooped distant missing mids | For a strongly scooped V-shaped sound. | 200:0 500:2.5 1000:4.5 2000:3 4500:0
Too distant | Body | recessed vocals far away | Brings instruments and voices closer. | 300:0 800:0.5 1500:1.5 2800:3 5000:1 9000:0
Too honky | Midrange | nasal horn megaphone | Removes the horn-like midrange peak. | 500:0 800:-2 1100:-3 1600:-1.5 2500:0
Too honky, strong | Midrange | nasal horn megaphone | A bigger cut for a strong midrange honk. | 450:0 800:-3.5 1100:-5 1700:-2.5 2800:0
Too nasal | Midrange | pinched voice blocked nose | Takes the pinched tone out of voices. | 600:0 1000:-2.5 1400:-2.5 2000:-1 3000:0
Too shouty | Midrange | aggressive forward screaming | Eases upper mids that feel like they're shouting. | 1000:0 1800:-2 2800:-3 4200:-1.5 6000:0
Too shouty, strong | Midrange | aggressive forward screaming | A bigger cut for very forward upper mids. | 900:0 1800:-3.5 2800:-5 4300:-2.5 6500:0
Vocals too quiet | Midrange | buried recessed singer | Brings buried vocals forward. | 250:-1 700:0 1500:1 2800:3.5 5000:1.5 9000:0
Vocals too loud | Midrange | forward singer over band | Sets vocals back into the band. | 500:0 1200:-1 2500:-3 4500:-1.5 8000:0
Too harsh | Treble | piercing sharp painful | Softens the most common harshness range. | 2000:0 3200:-2 4500:-3 6500:-1.5 9000:0
Too harsh, strong | Treble | piercing sharp painful | A deeper cut for painful peaks. | 1800:0 3200:-3.5 4500:-5.5 6500:-3 10000:0
Too sibilant | Treble | ess hiss s sounds | Softens S and T sounds. | 4000:0 6000:-2 7500:-3.5 9500:-2 13000:0
Too sibilant, strong | Treble | ess hiss s sounds | A deeper cut for sharp S sounds. | 3500:0 6000:-3.5 7500:-6 10000:-3 14000:0
Cymbals too splashy | Treble | hi hats tizzy crash | Softens brittle cymbals. | 5000:0 8000:-2 11000:-3 14000:-3
Too tinny | Treble | small thin metallic | Takes out metallic ring and adds body. | 150:2 300:2 800:0 2500:-0.5 5500:-2.5 9000:-2 14000:-1
Too fatiguing | Treble | tiring ears long listening | Gentle relief across the upper range. | 1500:0 3000:-1.5 6000:-2.5 10000:-2 15000:-1
Too bright | Treble | sharp thin shrill | A general reduction in treble. | 1500:0 4000:-1.5 8000:-3 14000:-4
Too bright, strong | Treble | sharp thin shrill | A strong reduction in treble. | 1200:0 4000:-3 8000:-5.5 14000:-7
Too dull | Treble | muffled dark lifeless | Brings the life back to the top end. | 1500:0 4000:1.5 8000:3 14000:4
Too dull, strong | Treble | muffled dark lifeless | For very muffled sound. | 1200:0 4000:3 8000:5.5 14000:7
Muffled | Treble | veiled blanket covered | Lifts the veil from the upper mids up. | 300:-1 1000:0 2500:2.5 5000:3.5 10000:3 15000:2.5
No air | Treble | closed in no sparkle | Adds a little openness at the very top. | 7000:0 11000:2 14000:3.5 18000:4
No detail | Treble | flat blurred lifeless | Lifts small details in the upper mids and highs. | 1200:0 2500:1 4500:2 8000:2 13000:2.5
Hiss | Noise | tape noise old recordings | Reduces constant high-frequency hiss. | 3000:0 6000:-2 9000:-4.5 13000:-7 16000:-9
Mains hum | Noise | buzz 50 hz 60 hz noise | A low cut for recordings with hum. | 30:-3 50:-7 70:-7 110:-3 200:0
Low bitrate streams | Noise | compressed mp3 artifacts swishy | Softens the swishy sound of heavy compression. | 200:0.5 1000:0 3000:0.5 6500:-2 10000:-4 15000:-6
Wind noise | Noise | outdoor recording rumble | Cuts low-frequency rumble from outdoor clips. | 40:-10 90:-7 170:-3 300:0
Room echo | Noise | echoey reverberant distant recording | Reduces low-mid build-up in echoey recordings. | 120:-3 250:-3 500:-1.5 1000:0 3000:2 7000:0.5
Earbuds lost their seal | Fit | loose fit tips no bass leak | Partly makes up for bass lost to a poor fit. Try other tips first. | 40:7 80:6 160:3.5 350:1 700:0
Too loud and dense | Dynamics | compressed brickwall crushed | Eases the bands that make loud masters tiring. | 80:-1 250:0 1000:0 3000:-1.5 6500:-1.5 12000:-0.5
Left the bass too high | Dynamics | undo reduce extra bass overload | Backs off bass that was added elsewhere. | 40:-4 80:-3.5 160:-2 320:-0.5 600:0
"""

    private const val gear = """
Basic earbuds | Earbuds | cheap wired stock | Common fixes: more bass, less glare. | 40:4.5 90:3.5 250:0.5 1000:0 3500:-1.5 7000:-1 13000:1.5
Bass-heavy earbuds | Earbuds | true wireless consumer | Reduces extra bass and restores mids. | 40:-3 90:-3 220:-1 800:1 2500:1 6000:0.5 13000:1
Bright earbuds | Earbuds | sharp true wireless | Softens a sharp top end. | 80:1 300:0 1500:0 3500:-1 7000:-2.5 13000:-1.5
Open-fit earbuds | Earbuds | airpods style no seal | Adds the low end that an unsealed fit loses. | 40:6 80:5.5 170:3 400:0.5 1000:0 6000:0 13000:1
Noise-cancelling earbuds | Earbuds | anc true wireless | Reins in the usual low-end lift and adds air. | 40:-1 90:-1.5 250:0 1000:0 3000:0.5 7000:0 14000:1.5
Sports earbuds | Earbuds | ear hook workout | Tighter bass and clearer vocals. | 40:1.5 90:-0.5 250:-1 1000:0 2800:1.5 7000:0.5 13000:1
In-ear monitors | Earbuds | iem wired | Light correction for a typical bright, lean tuning. | 40:2 100:1 400:0 1500:0 3500:-0.5 7000:-1 13000:0.5
Over-ear, bass-heavy | Headphones | wireless consumer closed | Eases boom and lifts the upper mids. | 40:-2 90:-3 220:-1.5 800:0.5 2800:2 7000:0.5 13000:1
Over-ear, warm | Headphones | dark relaxed closed | Adds clarity to a dark headphone. | 90:-1 300:-0.5 1000:0 3000:2 7000:2.5 13000:2.5
Over-ear, bright | Headphones | analytical open | Adds warmth and softens the top. | 40:3 100:2 300:0.5 1200:0 4000:-1 7500:-2.5 14000:-1
Open-back headphones | Headphones | audiophile hifi | Adds sub bass that open designs tend to lack. | 25:5 50:4 100:1.5 200:0 1000:0 6500:-0.5 14000:0
Closed-back headphones | Headphones | studio boxy cup | Clears up a boxy cup sound and adds air. | 60:0 180:-0.5 400:-1.5 800:-0.5 3000:0.5 8000:0.5 14000:2
On-ear headphones | Headphones | portable small | Adds lows and eases midrange honk. | 40:3 90:2 300:0 1000:-1 2500:0 7000:0.5 13000:1
Gaming headset | Headphones | mic boomy | Reduces mid bass bloat for clearer detail. | 50:0.5 110:-3 250:-2 700:0 2800:1.5 6000:1.5 13000:1
Bone conduction | Headphones | open ear running | Fills in bass and softens midrange buzz. | 50:8 100:7 220:4 600:0 1500:-2 3500:-1 7000:1 13000:3
Studio monitors at home | Speakers | desk nearfield | A light lift for very small desks and rooms. | 40:2.5 80:1 200:-0.5 800:0 3000:0 8000:0 14000:1
Phone speaker | Speakers | mobile small built in | Trims lows it can't play and adds midrange body. | 60:-12 120:-7 250:-0.5 500:2.5 1500:1 3200:-1.5 8000:1 14000:2
Tablet speaker | Speakers | ipad small built in | Fills the lower mids and softens glare. | 60:-9 120:-4 250:1 500:2 1500:0.5 3200:-1 8000:0.5 14000:1.5
Laptop speakers | Speakers | notebook small built in | Trims low strain and fills in low mids. | 60:-9 130:-3 280:2 600:1 1500:0 3500:-1.5 8000:0.5 14000:1.5
Small Bluetooth speaker | Speakers | portable wireless mini | Adds warmth and softens a boxy tone. | 60:-3 120:1.5 250:1.5 500:-1.5 1500:0 3500:0.5 8000:1 14000:2.5
Big Bluetooth speaker | Speakers | party portable | Tones down heavy bass and adds top end. | 50:-2 100:-1.5 250:-0.5 800:0 3000:1 8000:1.5 14000:2.5
Smart speaker | Speakers | echo home nest | Clears the lower mids and adds treble. | 60:-1 150:-1.5 350:-1 1000:0 3000:1 8000:2 14000:3
Soundbar | Speakers | tv living room | Clears speech and adds top end. | 50:0.5 150:-1 400:-1 1200:0 3000:2.5 8000:1 14000:1.5
TV speakers | Speakers | built in living room | Fills in the low end and lifts speech. | 70:-4 150:1 300:2 800:0 3000:3 8000:1 14000:0
Car stereo | Speakers | doors vehicle | Evens out door-panel boom and adds clarity. | 40:1.5 90:-1 200:-2.5 500:-1 1500:0 4000:1.5 10000:2.5
"""

    private const val character = """
Warm and smooth | Warm | mellow relaxed | Rich low mids with a soft top. | 60:2 200:2 800:0 3000:-1 8000:-2 14000:-2.5
Warm blanket | Warm | cosy dark soft | Very cosy and soft around the edges. | 60:3 200:3 700:0.5 2500:-1.5 6000:-3 13000:-4.5
Velvet | Warm | silky smooth rich | Smooth upper mids with a rich low end. | 70:2 200:1 700:0 3000:-1.5 7000:-1 14000:0.5
Tube warmth | Warm | valve amplifier analog | Gentle lift in the low mids and softer treble. | 100:1 250:2 800:0.5 3000:0 8000:-1 14000:-2
Vinyl | Warm | record turntable analog | Warm, slightly rolled-off record sound. | 30:-3 60:1 200:2 800:0.5 3000:0 8000:-1 13000:-3.5
Tape | Warm | cassette reel analog saturation | A soft bump in the lows and rolled-off highs. | 40:-1 90:2.5 200:1.5 800:0 4000:-0.5 9000:-2 14000:-4.5
Cassette | Warm | walkman lofi retro | Lean top with a soft bass hump. | 40:-3 100:2.5 300:1 1000:0 4000:-1 8000:-4 13000:-7
Fireside | Warm | acoustic intimate cosy | Intimate and woody, without any glare. | 80:1.5 220:2.5 700:0 2500:-0.5 6000:-1.5 13000:-2
Dark | Dark | mellow moody shaded | Pulls the top end down overall. | 80:1.5 300:0.5 1200:0 3000:-1.5 7000:-3.5 14000:-5
Very dark | Dark | muted heavy shaded | A strong shaded sound for bright recordings. | 80:2 300:1 1000:0 2500:-2.5 6000:-5.5 14000:-8
Moody | Dark | night brooding atmospheric | Deep lows, shaded mids and a little air. | 45:3.5 120:2 400:-1 1500:-1 4000:-1 9000:-0.5 14000:1
Underwater | Dark | muffled effect submerged | A heavy muffled effect, for fun. | 100:3 300:2 800:-2 1500:-6 3000:-10 8000:-12
Next door | Dark | wall party effect muffled | Sounds like music from the next room. | 60:3 150:3 400:0 800:-4 2000:-9 5000:-12
Bright and open | Bright | airy detail clear | Open and clear without much lift in the lows. | 80:0 300:-0.5 1000:0 3000:1 8000:2.5 14000:3.5
Crystal | Bright | sparkle glass shimmer | Sparkle at the very top. | 1000:0 5000:0.5 9000:2 13000:3.5 17000:4.5
Airy | Bright | open breath sparkle | Adds breath and space from 10 kHz up. | 3000:0 8000:1 12000:2.5 16000:4
Sparkling | Bright | shiny lively | A lively, slightly glassy treble. | 1500:0 4000:1 7000:2.5 11000:2 15000:3
Hi-fi | Bright | audiophile detail reference | Slightly clearer, with a mild smile. | 45:1.5 150:0.5 700:-0.5 2500:0 8000:1 14000:2
Studio reference | Neutral | mixing mastering flat near | Very light, with a slightly tidied low end. | 40:0.5 150:0 300:-0.5 1000:0 3500:0.5 8000:0 14000:0.5
Natural | Neutral | realistic honest | Only a small nudge towards realism. | 60:0.5 200:0.5 800:0 2500:0.5 7000:0 14000:0.5
Balanced | Neutral | even all rounder | Small corrections for general use. | 45:1.5 120:1 400:-0.5 1200:0 3500:0.5 8000:0.5 14000:1
All-rounder | Neutral | everyday default daily | A gentle, safe starting point for most music. | 50:2 120:1.5 400:-0.5 1500:0 3500:0.5 8000:1 14000:1.5
Gentle smile | Neutral | subtle v | A very mild lift at both ends. | 50:2 200:0.5 800:-0.5 3000:0 8000:1 14000:2
Consumer tuning | Neutral | fun mainstream | Enjoyable rather than strictly accurate. | 40:4 90:3 250:0.5 800:-0.5 3000:1 8000:1 14000:2.5
Preference-style bass | Neutral | target research sub low shelf | Bass shelf similar to popular preference research. Not a measured target. | 40:4 80:3.5 150:1.5 300:0 1000:0 3000:0.5
Big V | Fun | smile extreme scooped | Very strong bass and treble with sunken mids. | 40:7 90:5.5 250:1 800:-3.5 2500:-1 7000:3.5 14000:6
Bass cannon | Fun | basshead extreme slam | As much bass as is sensible. Watch the volume. | 30:11 60:10 120:6 250:1.5 500:0
Basshead | Fun | big deep extreme | Heavy bass with enough treble to stay clear. | 30:8 60:8 120:5 300:0.5 1000:-1 6000:1 14000:2
Club in your head | Fun | night dance venue | The heavy lows and bright highs of a dance floor. | 35:7 70:6 150:2 500:-2.5 2000:-0.5 8000:2.5 14000:4
Stadium | Fun | concert big arena live | Big and open, with a touch less midrange. | 40:4 90:3 300:-1 1000:-1 3000:0.5 8000:2 14000:3
Live concert | Fun | gig show band | Forward drums and vocals with bright cymbals. | 50:3 110:2 400:-1 1500:0 3000:2 8000:2 14000:2
Jazz club | Fun | intimate small room | Small-room warmth, soft and close. | 70:2 200:1.5 700:0 2000:0 6000:-0.5 14000:0
Cathedral | Fun | big space church | Deeper lows and an airy top. | 40:3 100:1.5 400:-1 1200:-1 3500:0 8000:1 14000:3
Headroom saver | Fun | safe no boost cuts only | Only cuts, so it can't add distortion. | 100:0 300:-2 800:-3 2000:-1.5 4000:0
Old radio | Effects | am wireless retro | The narrow sound of an old radio. | 100:-12 250:-5 500:1 1500:4 3000:3 5000:-5 8000:-12
Telephone | Effects | landline narrow call | The narrow sound of a phone line. | 200:-12 350:-3 700:2 2000:4 3200:0 4500:-10 7000:-12
Megaphone | Effects | bullhorn tannoy | Harsh, honking midrange. | 300:-12 600:-2 1200:6 2500:5 4500:-6 8000:-12
Gramophone | Effects | phonograph 1920s antique | The thin and boxy sound of early recordings. | 200:-12 400:-4 800:3 1600:5 3000:2 5000:-8 9000:-12
Old TV | Effects | crt vintage set | A small, boxy speaker effect. | 120:-12 300:-3 700:2.5 2200:3.5 5000:-3 9000:-12
Lo-fi crunch | Effects | bitcrush low quality retro | Narrow, dusty and rolled off on both ends. | 60:-6 150:-1 500:1.5 1500:2 3500:0 6000:-5 12000:-11
Bass only | Effects | study learn isolate low | Isolates the bass region for learning parts. | 60:6 200:6 400:-2 800:-8 1600:-12
Treble only | Effects | isolate cymbals high | Isolates the upper range. | 500:-12 1000:-8 2000:-3 4000:3 8000:6 16000:6
Vocal isolation | Effects | karaoke voice focus band | Focuses on the vocal region. | 80:-12 200:-8 500:-1 1500:3 3000:5 5000:2 9000:-5 14000:-10
Karaoke helper | Effects | voice backing sing | Lowers the vocal region to make room to sing. Doesn't remove vocals. | 200:0 500:-1.5 1200:-4.5 2800:-6 5000:-2 9000:0
"""
}
