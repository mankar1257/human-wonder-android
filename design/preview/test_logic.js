// Runs the chat artboard's logic (send, stream, stop, tones, perspective) without a browser.
// Usage: node design/preview/test_logic.js
const fs = require('fs');
const path = require('path');
const html = fs.readFileSync(path.join(__dirname, 'project', 'Chat.dc.html'), 'utf8');
const src = html.split('data-dc-script')[1].split('>').slice(1).join('>').split('</script>')[0];
class DCLogic {
  constructor(props) { this.props = props || {}; }
  setState(u) { const p = typeof u === 'function' ? u(this.state) : u; this.state = Object.assign({}, this.state, p); }
}
const queue = [];
global.setTimeout = (f) => { queue.push(f); return queue.length; };
global.clearTimeout = () => {};
global.setInterval = () => 1;
global.clearInterval = () => {};
global.performance = { now: () => 1000 };
const Component = eval('(function(){' + src + '; return Component;})()');
const drain = () => { let n = 0; while (queue.length && n++ < 2000) queue.shift()(); };
const assert = (cond, msg) => { if (!cond) { console.error('FAIL: ' + msg); process.exit(1); } };

const c = new Component({ name: 'Wanderer' });
c.state.sky = 'ready';
let v = c.renderVals();
assert(v.emptyDisplay === 'flex' && v.listDisplay === 'none', 'starts on the empty sky');
c.send('How do city lights look from 400km up?');
v = c.renderVals();
assert(v.messages.length === 2 && v.messages[1].acquiring, 'question sent, Orbit listening');
drain();
v = c.renderVals();
assert(!c.state.generating && v.messages[1].ended && v.messages[1].text.startsWith('Cities glow'), 'answer streamed in full');
c.setState({ perspective: 'ground', tone: 3 });
c.send('the moon'); drain();
assert(c.state.messages[3].text === 'Find the Moon low in the sky tonight, Wanderer.', 'ground + whisper');
c.send('stars?'); queue.shift()(); queue.shift()(); c.stop();
assert(!c.state.generating && c.state.messages[c.state.messages.length - 1].complete, 'stop ends the answer');
console.log('preview logic OK');
