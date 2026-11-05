package io.github.dragjon.eigentone.dsp

/**
 * Library entries, one per line: name | group | search words | description | hz:db ... | source, if any.
 *
 * Kept as text rather than Kotlin objects because a list this long would exceed the JVM's limit on
 * the size of one method. A unit test checks every line.
 *
 * Lines with a source come from published data. The rest are designed starting points. Shapes too close
 * to another were merged into it, and their names stay in its search words so they can still be found.
 */
internal object ShapeData {
    val blocks: List<Pair<ShapeCategory, String>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        listOf(
            ShapeCategory.REFERENCE to research,
            ShapeCategory.FIX to fixes,
            ShapeCategory.MUSIC to music1,
            ShapeCategory.MUSIC to music2,
            ShapeCategory.INSTRUMENT to instruments,
            ShapeCategory.MEDIA to media,
            ShapeCategory.SITUATION to situations,
            ShapeCategory.GEAR to gear,
            ShapeCategory.CHARACTER to character,
        )
    }

    private const val research = """
Quiet listening | Equal loudness | low volume loudness fletcher munson night iso | What ears lose when music is about 10 dB quieter than a loud reference. | 31.5:4.5 50:4 80:3.5 125:3 200:2 315:1.5 500:0.5 1000:0 2000:-0.5 4000:-0.5 8000:0 12500:1.5 | ISO 226:2003 equal-loudness contours
Low volume listening | Equal loudness | quiet loudness fletcher munson night iso | What ears lose about 20 dB down. Use Amount if it feels heavy. | 31.5:9.5 50:8 80:7 125:5.5 200:4 315:2.5 500:1 1000:0 2000:-0.5 4000:-0.5 8000:0.5 12500:3 | ISO 226:2003 equal-loudness contours
Very low volume | Equal loudness | whisper quiet loudness fletcher munson night iso | What ears lose about 30 dB down, limited to 12 dB. Use Amount if it feels heavy. | 31.5:12 50:12 80:10 125:8 200:6 315:3.5 500:1.5 1000:0 2000:-1 4000:-1 8000:0.5 12500:4.5 | ISO 226:2003 equal-loudness contours
Harman bass lift, over-ear | Headphone targets | harman target preference bass shelf research | The bass lift in the Harman over-ear 2018 target. For a correction aimed without it. | 20:6 31.5:6 50:5.5 80:4.5 125:2 200:0.5 315:0 500:0 | AutoEq published targets (MIT)
Harman bass lift, in-ear | Headphone targets | harman target preference bass shelf research iem earbuds | The bass lift in the Harman in-ear 2019 target. For a correction aimed without it. | 20:9 31.5:9 50:8.5 80:6.5 125:3 200:0.5 315:0 500:0 | AutoEq published targets (MIT)
More bass than Harman | Headphone targets | preference class research more bass | 5 dB of extra bass, in the 4 to 6 dB that 15% of tested listeners preferred. | 20:5 31.5:5 50:5 80:3.5 125:1.5 200:0.5 315:0 500:0 | Olive et al., Harman listener preference research
Less bass than Harman | Headphone targets | preference class research less bass | 2 dB less bass, which 21% of tested listeners preferred. | 20:-2 31.5:-2 50:-2 80:-1.5 125:-0.5 200:0 315:0 500:0 | Olive et al., Harman listener preference research
Harman to diffuse field | Headphone targets | df reference standard target neutral research | Moves a Harman correction to the older reference target: leaner bass and brighter treble. | 20:-7.5 31.5:-8 50:-7 80:-6 125:-4 200:-2 315:-1.5 500:-1 800:0 1000:0 1600:1.5 2500:3.5 4000:1 6300:0.5 8000:3 10000:4 12500:6.5 16000:6.5 | AutoEq published targets (MIT)
Harman to speakers in a room | Headphone targets | loudspeaker room target research flat | Moves a Harman correction towards flat speakers in a room: leaner bass, smoother upper mids. | 20:-8 31.5:-8 50:-7.5 80:-6.5 125:-3.5 200:-1.5 315:-1 500:-1 800:-0.5 1000:0 1600:0.5 2500:2 4000:1.5 6300:1.5 8000:1.5 10000:2 12500:2 16000:1 | AutoEq published targets (MIT)
Oratory1990 over-ear target | Headphone targets | oratory optimum hifi target research | Moves a Harman over-ear correction to oratory1990's target, with a smaller bass lift. | 20:-5.5 31.5:-5 50:-5 80:-3.5 125:-1.5 200:0.5 315:0 500:0 800:0 1000:0 1600:0 2500:0 4000:-0.5 6300:-0.5 8000:-1 10000:-1 12500:0 16000:0.5 | AutoEq published targets (MIT)
Oratory1990 in-ear target | Headphone targets | oratory iem target research warm | Moves a Harman in-ear correction to oratory1990's warmer target, with softer upper mids. | 20:0.5 31.5:0.5 50:1.5 80:3 125:3.5 200:3 315:2 500:1 800:0.5 1000:0 1600:-1.5 2500:-2 4000:-1.5 6300:-0.5 8000:0 10000:0.5 12500:1 16000:1 | AutoEq published targets (MIT)
Speech intelligibility | Speech | sii dialogue clarity voice understand words | Lifts each band by how much it matters to understanding speech, and clears bass that masks it. | 80:-4 125:-3 250:-0.5 500:1.5 1000:2.5 2000:3 4000:2.5 8000:0.5 | ANSI S3.5 speech intelligibility band weights
Speaker room tilt | Speakers | in room slope house curve flat speakers | The gentle fall of about 1 dB per octave that good speakers measure in a room. | 20:5.5 1000:0 20000:-4.5 | Olive and Toole, in-room loudspeaker response
"""

    private const val music1 = """
Pop | Pop | chart radio mainstream Indie pop alternative bedroom J-pop jpop japanese anime | Punchy low end and clear, forward vocals. Also suits Indie pop, J-pop. | 50:3 120:2.5 400:-1 2500:1.5 6000:1.5 12000:2.5
K-pop | Pop | kpop korean idol Modern pop chart streaming Dance pop club chart Synth pop 80s electropop | Dense, bright mixes with a firm low end. Also suits Modern pop, Dance pop, Synth pop. | 50:3 100:2 400:-1.5 2500:1 6000:2 13000:3
Ballad | Pop | slow love song emotional | Warm body with a close, present voice. | 80:1.5 200:1.5 500:-0.5 2500:2 5000:1 12000:1
Soft pop | Pop | easy listening mellow | Relaxed top end with a gentle lift in warmth. | 80:1.5 250:1 1000:0 4000:-0.5 12000:-1.5
Teen pop | Pop | bright upbeat | Bright, bouncy and vocal-led. | 60:2 200:0 800:0 3000:2 8000:2.5 14000:2.5
Rock | Rock & metal | guitar band Hard rock loud riffs Alternative rock alt indie Power metal melodic symphonic Metalcore breakdown screams | Weighty drums and guitars with bite. Also suits Hard rock, Alternative rock, Power metal and more. | 60:3 120:2 400:-1.5 1200:0.5 3000:2.5 8000:2 14000:1.5
Classic rock | Rock & metal | 70s vintage guitar Grunge 90s alternative | Warm and midrange-rich, without modern brightness. Also suits Grunge. | 70:2 200:1.5 800:1 2500:1.5 6000:0 14000:-1
Punk | Rock & metal | fast raw guitars Indie rock alternative jangly Emo pop punk | Raw midrange drive, without much extra bass. Also suits Indie rock, Emo. | 80:1 200:0 800:1 2500:3 5000:2 12000:0
Post-rock | Rock & metal | ambient guitars instrumental | Wide, slow builds with open highs. | 50:2 150:0.5 500:-1 2000:0 6000:1.5 14000:3
Shoegaze | Rock & metal | dream noise wall guitars Progressive rock prog complex | Tames the wall of guitars and lifts vocals out of it. Also suits Progressive rock. | 60:1.5 300:-1 1000:-1 2500:1.5 5000:-0.5 12000:2
Heavy metal | Rock & metal | guitars double kick | Scooped mids with fast kick and guitar edge. | 60:4 100:2.5 500:-3 1500:-0.5 3500:3 8000:2.5
Thrash metal | Rock & metal | fast riffs | Tighter low end so fast palm mutes stay clear. | 50:1 90:3 200:-0.5 500:-2.5 3000:3 6000:3 12000:1
Death metal | Rock & metal | growl blast beats | Separates low growls from dense guitars. | 50:2.5 100:3 300:-1 700:-3 2500:2.5 5000:3 10000:1
Black metal | Rock & metal | raw tremolo | Adds weight to thin mixes and softens fizz. | 60:4 150:2.5 500:0 2000:0 6000:-1.5 12000:-2.5
Djent | Rock & metal | prog metal tight | Very tight bass and hard attack in the upper mids. | 40:-1 80:3 160:2 400:-3 1200:-1 3000:3 6000:2
Doom metal | Rock & metal | stoner sludge slow | Huge low end, with fizz kept down. | 40:4 90:3 200:1.5 800:-1 3000:0.5 10000:-1.5
Hip-hop | Hip-hop & R&B | rap hiphop Drill uk rap sliding 808 Phonk drift cowbell memphis | Deep bass with a clear vocal on top. Also suits Drill, Phonk. | 40:4 80:3 180:1 500:-1 2500:1 8000:1 14000:1
Boom bap | Hip-hop & R&B | 90s rap samples | Round kick and a dusty, warm top. | 60:4 120:3 300:0 1000:0 3000:1 8000:-0.5 14000:-1
Trap | Hip-hop & R&B | 808 rap hi hats Alt R&B rnb moody atmospheric | Sub-heavy 808s with crisp hi-hats. Also suits Alt R&B. | 30:4 60:3.5 130:1 400:-1.5 2000:0 8000:1.5 14000:2.5
Lo-fi hip-hop | Hip-hop & R&B | study beats chill | Soft highs and warm, rounded bass. | 60:3 150:2 500:0 2000:-0.5 6000:-2 14000:-4
R&B | Hip-hop & R&B | rnb rhythm blues Disco 70s dance strings | Smooth low end and silky vocals. Also suits Disco. | 50:3.5 110:2.5 350:-1 1500:0.5 3500:1.5 12000:2
Soul | Hip-hop & R&B | motown vintage Neo soul rnb jazzy | Warm mids and rich, natural vocals. Also suits Neo soul. | 80:2 200:1.5 700:0.5 2500:1.5 6000:0 14000:-0.5
Funk | Hip-hop & R&B | groove slap bass Conscious rap lyrics wordplay hiphop Old school hip-hop 80s rap classic | Bouncy bass and snappy guitar. Also suits Conscious rap, Old school hip-hop. | 60:3 120:2.5 350:-1 1000:0 2500:2 7000:1.5 13000:1
Gospel | Hip-hop & R&B | choir church | Full choir warmth with a clear lead vocal. | 80:2 200:1.5 600:0 2500:2 6000:1 13000:1.5
"""

    private const val music2 = """
EDM | Electronic | festival dance big room Psytrance goa psychedelic | Big kick and bright leads. Also suits Psytrance. | 40:4 80:3.5 200:0 500:-2 2500:0.5 8000:2 14000:3
House | Electronic | deep dance club Minimal techno sparse hypnotic Garage uk 2 step speed Electro synth robotic | Steady kick with room left for vocals. Also suits Minimal techno, Garage, Electro. | 45:3.5 100:3 300:-1 1000:0 3000:1 9000:1.5 14000:2
Deep house | Electronic | chill dance Downtempo chillout trip hop | Warm, rolling bass with soft highs. Also suits Downtempo. | 45:4 100:3 300:0 1000:-0.5 4000:0 12000:0.5
Techno | Electronic | berlin warehouse Liquid drum and bass dnb smooth soulful | Driving kick and dark, hypnotic mids. Also suits Liquid drum and bass. | 40:4 80:3.5 200:0.5 600:-1.5 3000:0.5 9000:1.5 14000:1
Trance | Electronic | uplifting psy Tech house club groove Progressive house melodic trance Future bass melodic supersaw | Pumping low end with soaring leads. Also suits Tech house, Progressive house, Future bass. | 45:3.5 90:3 300:-1.5 1500:0.5 6000:2.5 14000:3.5
Drum and bass | Electronic | dnb jungle liquid | Deep sub with a fast, sharp snare. | 35:4 70:3 180:0 500:-1.5 2500:1 7000:2 13000:2
Dubstep | Electronic | wobble bass growl Jungle breakbeat dnb 90s | Massive sub with a midrange growl. Also suits Jungle. | 35:4 70:3.5 160:0.5 450:-1.5 1500:0.5 5000:1 13000:1.5
Synthwave | Electronic | retrowave outrun 80s | Round bass, wide pads and a soft glow. | 60:3 150:1.5 500:-1 2000:0.5 6000:1 12000:1.5
Vaporwave | Electronic | slow retro aesthetic | Dreamy, hazy and slightly dark. | 60:2.5 200:1.5 800:0.5 3000:-1 8000:-2.5 14000:-4
Hyperpop | Electronic | glitch pc music Trip-hop triphop 90s bristol | Keeps extreme highs in check and adds some body. Also suits Trip-hop. | 50:2.5 150:1.5 500:0 2500:0.5 6000:-1 12000:-1.5
Glitch | Electronic | idm experimental IDM braindance experimental | Sharp clicks and detail with a clean low end. Also suits IDM. | 50:2 150:0 500:-1 2500:0.5 8000:2.5 15000:3
Ambient | Electronic | atmospheric drone | Soft and spacious, with nothing sharp. | 50:2 200:0.5 800:-0.5 3000:-1 8000:0 14000:1
Reggaeton | Electronic | latin dembow perreo Hardstyle hardcore kick | Hard dembow kick with a bright vocal. Also suits Hardstyle. | 45:4 90:3 250:0 600:-2 2500:1.5 8000:1.5 14000:2
Jazz | Jazz & blues | swing quartet acoustic Smooth jazz sax lounge Big band swing brass orchestra Fusion electric jazz rock Vocal jazz crooner standards singer Bossa nova brazil latin guitar | Natural warmth and open cymbals. Also suits Smooth jazz, Big band, Fusion and more. | 60:1.5 200:1 600:-0.5 2000:0 6000:0.5 14000:1.5
Blues | Jazz & blues | delta electric guitar Bebop fast small group | Warm mids and singing guitar. Also suits Bebop. | 80:1.5 200:1.5 800:1 2500:1.5 6000:0 14000:-1
Delta blues | Jazz & blues | old acoustic 1930s | Adds some body to thin old recordings. | 100:3 250:2 800:0.5 2500:1 5000:-1 10000:-3
Classical | Classical | orchestra symphony Chamber music string quartet small Baroque harpsichord bach Choral choir sacred Minimalism contemporary repeating | Almost untouched, with a slight lift for air. Also suits Chamber music, Baroque, Choral and more. | 60:1 200:0.5 800:0 3000:0 8000:0.5 14000:1.5
Opera | Classical | soprano tenor aria | Clear voices over the orchestra. | 60:0.5 250:0 800:0.5 2800:2.5 5000:1 13000:0.5
Solo piano | Classical | keys recital | Full lower octaves and a clean, soft attack. | 60:1.5 200:1 700:-0.5 2500:0.5 7000:0.5 14000:0.5
Film score | Classical | soundtrack cinematic orchestra Symphony orchestra large romantic | Big low brass and wide, detailed strings. Also suits Symphony. | 45:3 120:1.5 500:-1 2000:0 7000:1 14000:2
Country | Folk & country | nashville twang Bluegrass banjo mandolin fiddle Folk acoustic singer songwriter Celtic irish fiddle whistle | Clear vocals and bright acoustic guitar. Also suits Bluegrass, Folk, Celtic. | 80:1.5 200:0.5 600:-0.5 2500:2 6000:1.5 13000:1.5
Classic country | Folk & country | honky tonk steel old Americana roots singer songwriter | Warm and honest, with a clear voice. Also suits Americana. | 90:1.5 250:1 800:0.5 2500:1.5 6000:0 13000:-0.5
Acoustic | Folk & country | unplugged guitar Indie folk acoustic bedroom gentle | Keeps instruments sounding natural and woody. Also suits Indie folk. | 80:1 200:1 600:-0.5 2500:0.5 7000:1 14000:1.5
Reggae | World | jamaica ska roots | Round, deep bass and an airy skank guitar. | 50:4 100:3.5 250:1 800:-1.5 3000:0.5 8000:0.5 14000:0
Dub | World | echo jamaica | Very deep bass and dark, spacious echoes. | 45:4 90:3.5 220:0.5 800:-1.5 3000:-0.5 8000:-0.5 14000:-0.5
Ska | World | offbeat horns Salsa latin brass cuba | Bright horns and a choppy guitar. Also suits Salsa. | 70:1.5 200:0 700:0 2500:2 6000:1.5 13000:1
Afrobeats | World | nigeria amapiano african | Warm kick and bright percussion. | 50:3.5 100:3 300:-0.5 1000:0 3000:1.5 8000:1.5 14000:2
Amapiano | World | log drum south africa | Deep log drums with soft, airy keys. | 40:4 80:3 180:1 500:-1 2500:0.5 8000:1 14000:1.5
Latin pop | World | spanish tropical Bachata latin guitar dominican Samba brazil percussion Bollywood hindi indian film | Warm percussion and a forward vocal. Also suits Bachata, Samba, Bollywood. | 60:2.5 150:1.5 500:-1 2500:1.5 7000:1.5 13000:2
Flamenco | World | spanish guitar | Sharp guitar attack and clear handclaps. | 90:0.5 250:0.5 800:0 3000:2 7000:1.5 13000:1
Indian classical | World | sitar tabla raga | Rich tabla and shimmering strings. | 70:1.5 200:1 700:0 2500:0.5 7000:1 14000:2
Arabic | World | oud middle east Fado portuguese voice guitar | Warm oud with clear, ornamented vocals. Also suits Fado. | 80:1 220:1 700:0.5 2500:1.5 6000:0.5 13000:0.5
Mandopop | World | chinese cantopop c-pop | Smooth, vocal-led and gently bright. | 70:1.5 200:0.5 600:-0.5 2800:1.5 7000:1 13000:2
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
Breathy vocals | Vocals | intimate whisper air Harmony vocals backing choir stacks | Adds air and closeness to soft singers. Also suits Harmony vocals. | 150:-1 500:-0.5 2000:0.5 6000:1 12000:3
Acoustic guitar | Guitar | strum fingerpick | Body plus string detail. | 100:0 220:1.5 500:-1 2500:1 6000:2 13000:1.5
Electric guitar | Guitar | amp distortion riff Lead guitar solo shred | More guitar edge and midrange bite. Also suits Lead guitar. | 100:-0.5 300:-1 900:1 2500:3 5000:1.5 10000:-0.5
Clean guitar | Guitar | jazz chorus jangle | Chime and sparkle for clean tones. | 120:0 400:-1 1500:0.5 3500:1.5 8000:2 13000:1
Classical guitar | Guitar | nylon spanish | Warm nylon tone without string squeak. | 100:0.5 220:1.5 700:0 2500:1 6000:-0.5 13000:0
Bass guitar | Bass | electric fingers low Upright bass double jazz acoustic | Round bass notes that are easy to follow. Also suits Upright bass. | 40:2 80:3 180:2.5 500:0 900:1 2500:0.5 8000:0
Slap bass | Bass | funk pop | Low thump plus string snap. | 40:2.5 90:3 250:-1 900:0 2500:2 5000:2 10000:0
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
Cello | Strings | solo warm Ukulele hawaii strum | Rich low strings and bow texture. Also suits Ukulele. | 70:1.5 180:2 400:1 1000:0 3000:1 8000:0.5
String section | Strings | orchestra ensemble | Silky and wide. | 100:0.5 350:-0.5 1000:0 3000:0 7500:1.5 14000:2.5
Harp | Strings | plucked glissando | Soft, glassy plucks. | 150:0.5 500:-0.5 1500:0 4000:1 8000:1.5 14000:2
Mandolin and banjo | Strings | bluegrass picked | Bright attack without getting thin. | 150:0.5 500:0 1500:0.5 3500:2 7000:1.5 13000:0
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
Bus ride | Travel | engine commute | Lifts low mids and upper mids over engine drone. | 50:2 110:3.5 250:2.5 700:0.5 2500:2 7000:1 13000:0
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
Falling asleep | Night | bed sleep calm | Dark and soft, with almost no top end. | 60:1.5 200:1.5 800:0 2000:-2 5000:-5 12000:-8
Sleep sounds | Night | rain white noise | Softens hissy rain and noise tracks. | 60:2 200:1 800:0 2500:-1.5 6000:-4 14000:-7
Softer treble | Comfort | sensitive ears gentle treble soft | A gentle treble reduction for sensitive ears. | 1000:0 3000:-1 6000:-2.5 10000:-3.5 14000:-4
Long sessions | Comfort | fatigue all day | Eases the region that tends to tire ears. | 80:0.5 300:0 1000:0 3000:-1 6500:-2 13000:-1
Quiet room | Comfort | home silent critical Noise cancelling on anc earbuds quiet | Clean and detailed when nothing masks the music. Also suits Noise cancelling on. | 50:0.5 300:-0.5 1000:0 3000:0 8000:0.5 14000:1
Transparency mode on | Comfort | ambient aware earbuds | Makes up for the thinner bass of transparency mode. | 40:3 90:3 200:1.5 700:0 2500:0.5 8000:0 14000:0
"""

    private const val fixes = """
Too muddy | Low end | muddy unclear cloudy strong severe very | Clears the build-up that blurs bass and low mids. | 100:0 200:-2 300:-3 500:-1.5 900:0
Too boomy | Low end | boomy bloated one note bass strong severe very | Tames bass that hangs around. | 40:-1 80:-3 140:-3.5 250:-1 500:0
Rumble | Low end | subsonic thump rumble noise | Rolls off the very lowest notes. | 25:-9 40:-6 70:-2 120:0
Weak bass | Low end | thin no bass lacks strong severe very | Adds fullness where bass feels missing. | 40:3.5 80:3.5 160:2.5 300:0.5 500:0
No punch | Low end | kick impact soft bass flat | Adds impact where kick drums land. | 50:0.5 90:3 130:3.5 220:1 400:-0.5 700:0
No sub bass | Low end | deep rumble missing | Extends the lowest octave only. | 25:5 45:4.5 80:1.5 130:0
Too thin | Body | weak lean bright no body strong severe very | Adds body to the low mids. | 80:1 180:2.5 300:3 600:1 1200:0
Too boxy | Body | cardboard closed in strong severe very | Removes the "inside a box" sound. | 200:0 350:-2 500:-3 800:-1.5 1300:0
Too hollow | Body | scooped distant missing mids strong severe very | Restores mids that seem to be missing. | 250:0 500:1.5 1000:3 2000:2 4000:0
Too distant | Body | recessed vocals far away | Brings instruments and voices closer. | 300:0 800:0.5 1500:1.5 2800:3 5000:1 9000:0
Too honky | Midrange | nasal horn megaphone strong severe very | Removes the horn-like midrange peak. | 500:0 800:-2 1100:-3 1600:-1.5 2500:0
Too nasal | Midrange | pinched voice blocked nose | Takes the pinched tone out of voices. | 600:0 1000:-2.5 1400:-2.5 2000:-1 3000:0
Too shouty | Midrange | aggressive forward screaming strong severe very Vocals too loud forward singer over band | Eases upper mids that feel like they're shouting. Also suits Vocals too loud. | 1000:0 1800:-2 2800:-3 4200:-1.5 6000:0
Vocals too quiet | Midrange | buried recessed singer | Brings buried vocals forward. | 250:-1 700:0 1500:1 2800:3.5 5000:1.5 9000:0
Too harsh | Treble | piercing sharp painful strong severe very | Softens the most common harshness range. | 2000:0 3200:-2 4500:-3 6500:-1.5 9000:0
Too sibilant | Treble | ess hiss s sounds strong severe very | Softens S and T sounds. | 4000:0 6000:-2 7500:-3.5 9500:-2 13000:0
Cymbals too splashy | Treble | hi hats tizzy crash | Softens brittle cymbals. | 5000:0 8000:-2 11000:-3 14000:-3
Too tinny | Treble | small thin metallic | Takes out metallic ring and adds body. | 150:2 300:2 800:0 2500:-0.5 5500:-2.5 9000:-2 14000:-1
Too fatiguing | Treble | tiring ears long listening | Gentle relief across the upper range. | 1500:0 3000:-1.5 6000:-2.5 10000:-2 15000:-1
Too bright | Treble | sharp thin shrill strong severe very | A general reduction in treble. | 1500:0 4000:-1.5 8000:-3 14000:-4
Too dull | Treble | muffled dark lifeless strong severe very | Brings the life back to the top end. | 1500:0 4000:1.5 8000:3 14000:4
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
Tablet speaker | Speakers | ipad small built in Laptop speakers notebook small built in | Fills the lower mids and softens glare. Also suits Laptop speakers. | 60:-9 120:-4 250:1 500:2 1500:0.5 3200:-1 8000:0.5 14000:1.5
Small Bluetooth speaker | Speakers | portable wireless mini | Adds warmth and softens a boxy tone. | 60:-3 120:1.5 250:1.5 500:-1.5 1500:0 3500:0.5 8000:1 14000:2.5
Big Bluetooth speaker | Speakers | party portable | Tones down heavy bass and adds top end. | 50:-2 100:-1.5 250:-0.5 800:0 3000:1 8000:1.5 14000:2.5
Smart speaker | Speakers | echo home nest | Clears the lower mids and adds treble. | 60:-1 150:-1.5 350:-1 1000:0 3000:1 8000:2 14000:3
Soundbar | Speakers | tv living room | Clears speech and adds top end. | 50:0.5 150:-1 400:-1 1200:0 3000:2.5 8000:1 14000:1.5
TV speakers | Speakers | built in living room | Fills in the low end and lifts speech. | 70:-4 150:1 300:2 800:0 3000:3 8000:1 14000:0
Car stereo | Speakers | doors vehicle | Evens out door-panel boom and adds clarity. | 40:1.5 90:-1 200:-2.5 500:-1 1500:0 4000:1.5 10000:2.5
"""

    private const val character = """
Warm and smooth | Warm | mellow relaxed Fireside acoustic intimate cosy | Rich low mids with a soft top. Also suits Fireside. | 60:2 200:2 800:0 3000:-1 8000:-2 14000:-2.5
Warm blanket | Warm | cosy dark soft | Very cosy and soft around the edges. | 60:3 200:3 700:0.5 2500:-1.5 6000:-3 13000:-4.5
Velvet | Warm | silky smooth rich | Smooth upper mids with a rich low end. | 70:2 200:1 700:0 3000:-1.5 7000:-1 14000:0.5
Tube warmth | Warm | valve amplifier analog | Gentle lift in the low mids and softer treble. | 100:1 250:2 800:0.5 3000:0 8000:-1 14000:-2
Vinyl | Warm | record turntable analog | Warm, slightly rolled-off record sound. | 30:-3 60:1 200:2 800:0.5 3000:0 8000:-1 13000:-3.5
Tape | Warm | cassette reel analog saturation | A soft bump in the lows and rolled-off highs. | 40:-1 90:2.5 200:1.5 800:0 4000:-0.5 9000:-2 14000:-4.5
Cassette | Warm | walkman lofi retro | Lean top with a soft bass hump. | 40:-3 100:2.5 300:1 1000:0 4000:-1 8000:-4 13000:-7
Dark | Dark | mellow moody shaded | Pulls the top end down overall. | 80:1.5 300:0.5 1200:0 3000:-1.5 7000:-3.5 14000:-5
Very dark | Dark | muted heavy shaded | A strong shaded sound for bright recordings. | 80:2 300:1 1000:0 2500:-2.5 6000:-5.5 14000:-8
Moody | Dark | night brooding atmospheric | Deep lows, shaded mids and a little air. | 45:3.5 120:2 400:-1 1500:-1 4000:-1 9000:-0.5 14000:1
Underwater | Dark | muffled effect submerged | A heavy muffled effect, for fun. | 100:3 300:2 800:-2 1500:-6 3000:-10 8000:-12
Next door | Dark | wall party effect muffled | Sounds like music from the next room. | 60:3 150:3 400:0 800:-4 2000:-9 5000:-12
Bright and open | Bright | airy detail clear Sparkling shiny lively | Open and clear without much lift in the lows. Also suits Sparkling. | 80:0 300:-0.5 1000:0 3000:1 8000:2.5 14000:3.5
Airy | Bright | open breath sparkle Crystal sparkle glass shimmer | Adds breath and space from 10 kHz up. Also suits Crystal. | 3000:0 8000:1 12000:2.5 16000:4
Hi-fi | Bright | audiophile detail reference | Slightly clearer, with a mild smile. | 45:1.5 150:0.5 700:-0.5 2500:0 8000:1 14000:2
Natural | Neutral | realistic honest Studio reference mixing mastering flat near | Only a small nudge towards realism. Also suits Studio reference. | 60:0.5 200:0.5 800:0 2500:0.5 7000:0 14000:0.5
Balanced | Neutral | even all rounder All-rounder everyday default daily Gentle smile subtle v | Small corrections for general use. Also suits All-rounder, Gentle smile. | 45:1.5 120:1 400:-0.5 1200:0 3500:0.5 8000:0.5 14000:1
Consumer tuning | Neutral | fun mainstream | Enjoyable rather than strictly accurate. | 40:4 90:3 250:0.5 800:-0.5 3000:1 8000:1 14000:2.5
Big V | Fun | smile extreme scooped | Very strong bass and treble with sunken mids. | 40:7 90:5.5 250:1 800:-3.5 2500:-1 7000:3.5 14000:6
Bass cannon | Fun | basshead extreme slam | As much bass as is sensible. Watch the volume. | 30:11 60:10 120:6 250:1.5 500:0
Basshead | Fun | big deep extreme | Heavy bass with enough treble to stay clear. | 30:8 60:8 120:5 300:0.5 1000:-1 6000:1 14000:2
Club in your head | Fun | night dance venue | The heavy lows and bright highs of a dance floor. | 35:7 70:6 150:2 500:-2.5 2000:-0.5 8000:2.5 14000:4
Stadium | Fun | concert big arena live Cathedral big space church | Big and open, with a touch less midrange. Also suits Cathedral. | 40:4 90:3 300:-1 1000:-1 3000:0.5 8000:2 14000:3
Jazz club | Fun | intimate small room | Small-room warmth, soft and close. | 70:2 200:1.5 700:0 2000:0 6000:-0.5 14000:0
Headroom saver | Fun | safe no boost cuts only Live concert gig show band | Only cuts, so it can't add distortion. Also suits Live concert. | 100:0 300:-2 800:-3 2000:-1.5 4000:0
Old radio | Effects | am wireless retro | The narrow sound of an old radio. | 100:-12 250:-5 500:1 1500:4 3000:3 5000:-5 8000:-12
Telephone | Effects | landline narrow call | The 300 Hz to 3.4 kHz band of a traditional phone line. | 150:-12 300:-3 500:1 2000:3 3400:0 4500:-10 7000:-12 | ITU-T narrowband telephone range
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
