# Orbit's archive: photos and loops in the chat

Orbit 412 can send a picture with an answer: a real astronaut photograph of Earth taken from the
International Space Station, which flies at about 410 km, almost exactly Orbit's height. Short
loops (the "GIFs") are cut from the station's time-lapse sequences. Everything ships inside the app;
nothing is fetched at run time (the app has no internet permission).

## Files

| File | What it is |
|---|---|
| `manifest.txt` | Which NASA frames to download (photo ids and time-lapse ranges). |
| `../../Get Orbit Photos.command` | Double-click on the Mac: downloads the manifest into `media-src/` (not in git). |
| `frames.json` | The catalog both apps read: id, kind (photo/loop), caption, what it shows (`says`), keywords. |
| `pick.py` | Reference picker. `python3 tools/frames/pick.py` runs `pick-cases.json`. |
| `pick-cases.json` | Questions and the photo Orbit should send (also run by the Android unit test). |
| `model_lab.py` | How the app's own model was tested (see results below). |
| `build_assets.py` | Turns `media-src/` into the app files (JPEG photos, animated WebP loops). |

## Credit and licence

NASA material is not protected by copyright in the United States. The Gateway to Astronaut
Photography of Earth asks for this credit, which the app shows in the photo viewer and on the
licences screen: "Image courtesy of the Earth Science and Remote Sensing Unit, NASA Johnson Space
Center" (https://eol.jsc.nasa.gov). No NASA logos and no recognisable people are used, and nothing
suggests NASA endorses the app.

Orbit calls them frames "from my archive", never photos it took itself.

## Why the app picks the photo, not the model

Tested on 2026-11-13 with the app's model file (hw-core.gguf) through llama-server, using the same
sampling as the app (temperature 0.7, top-k 20, top-p 0.8, repeat penalty 1.1). There were 60
questions: 50 that have a fitting photo and 10 that should get none.

| How the photo is chosen | Right photo (50 with a photo) | Notes |
|---|---|---|
| Model ends its answer with `[frame: id]` from a list in its prompt | 7 of 100 (2 runs) | Invented ids such as "504". 72 of 120 answers had stray brackets. |
| Model answers a separate, grammar-limited question (an id or "none") | 3 of 50 | Said "none" 45 times, even for "Show me Abu Dhabi". |
| Same, but it must pick an id | 7 of 50 | Picked "milky-way-loop" 50 times out of 60. |
| **App rules (`pick.py`, keyword scores)** | **50 of 50** | All 60 cases right, including Arabic and Hindi. |

So the app chooses, and the model presents what was chosen.

## How the model presents it

When a photo is attached, the question sent to the model gets one more hidden line:
"You are sending them a frame from your archive that shows <says>. Begin by telling them you are
sending a frame from your archive, then describe it." The person never sees this line.

| Prompt | Says it is sending a frame | Describes what the photo shows | Copies the prompt's example answer |
|---|---|---|---|
| Old prompt, no photos | n/a | n/a | 24 of 60 |
| Old prompt + photo line | 28 of 50 | 33 of 50 | 36 of 60 |
| **New prompt (no example answer) + photo line** | **31 of 50** | **46 of 50** | **0 of 60** |
| New prompt, no photos | n/a | n/a | 0 of 60 |

The example answer in the old system prompt was being copied almost word for word ("the coast glows
like a string of gold beads") in 40% of all answers, even about Delhi or the Moon. Without it the
model writes about what was asked, and with a photo it describes that photo. The new prompt also
says "Sometimes begin with a short radio phrase, like "Reading you clearly."" (the longer phrase was
being repeated at the start of most answers).

Without the example the model uses Markdown and line breaks more often, and sometimes repeats the
hidden line ("You are sending a frame…"). `Persona.visibleAnswer()` removes these on both platforms.
The card under the answer always says "From Orbit's archive", so the photo is presented even when the
model forgets to mention it.
