// Requires minecraft-protocol in NODE_PATH. Connect only to a disposable probe server.
const mc = require('minecraft-protocol');
const [version, port, index] = process.argv.slice(2);
if (!version || !port || !index) {
    throw new Error('Usage: node tools/check-absorption-client.cjs <version> <port> <absorption-index>');
}
// Full pool, half pool, automatic raw fallback, re-enabled scale, config-disabled raw.
const expected = [20, 10, 150, 20, 150];
const seen = [];
let entityId;
let passed = false;
const client = mc.createClient({
    host: '127.0.0.1', port: Number(port), username: 'AbsorptionProbe', auth: 'offline', version
});
const timeout = setTimeout(() => {
    console.error('ABS_CLIENT_TIMEOUT', JSON.stringify(seen));
    client.end();
    process.exitCode = 1;
}, 20000);
client.on('packet', (data, metadata) => {
    if (metadata.name === 'login') {
        entityId = data.entityId;
    }
    if (metadata.name !== 'entity_metadata' || data.entityId !== entityId) {
        return;
    }
    const absorption = data.metadata.find(value => value.key === Number(index));
    if (!absorption) return;
    console.log('ABS_CLIENT_META', absorption.value);
    if (absorption.value === expected[seen.length]) {
        seen.push(absorption.value);
    }
    if (seen.length === expected.length) {
        passed = true;
        clearTimeout(timeout);
        console.log('ABS_CLIENT_PASS', JSON.stringify(seen));
        client.end();
    }
});
client.on('error', error => {
    clearTimeout(timeout);
    console.error(error.message);
    process.exitCode = 1;
});
client.on('end', () => {
    clearTimeout(timeout);
    if (!passed) process.exitCode = 1;
});
