import test from "node:test";
import assert from "node:assert/strict";
import {consumeSse} from "../../src/main/resources/static/sse.mjs";

const utf8 = new TextEncoder();
function stream(bytes, chunkSize = 1) {
    return new ReadableStream({start(controller) {
        for (let pos = 0; pos < bytes.length; pos += chunkSize) controller.enqueue(bytes.slice(pos, pos + chunkSize));
        controller.close();
    }});
}

test("UTF-8 characters and CRLF frame boundaries may split at every byte", async () => {
    const events = [];
    await consumeSse(stream(utf8.encode(': ping\r\nevent: delta\r\ndata: {"text":"中文🙂"}\r\n\r\nevent: result\ndata: {}\n\n')),
        (event, data) => events.push([event, data]));
    assert.deepEqual(events, [["delta", '{"text":"中文🙂"}'], ["result", "{}"]]);
});

test("multiple frames per chunk and multiline data", async () => {
    const events = [];
    await consumeSse(stream(utf8.encode('event: status\ndata: one\ndata: two\n\nevent: result\ndata: {}\n\n'), 200),
        (event, data) => events.push([event, data]));
    assert.deepEqual(events, [["status", "one\ntwo"], ["result", "{}"]]);
});

test("EOF drops a terminal-looking frame without a blank line", async () => {
    const events = [];
    await consumeSse(stream(utf8.encode('event: result\ndata: {}\n')), (event, data) => events.push([event, data]));
    assert.deepEqual(events, []);
});

test("application terminal event cancels the reader and ignores later frames", async () => {
    const events = [];
    await consumeSse(stream(utf8.encode('event: result\ndata: {}\n\nevent: delta\ndata: stale\n\n'), 200), (event, data) => {
        events.push([event, data]);
        return false;
    });
    assert.deepEqual(events, [["result", "{}"]]);
});

test("oversized frames and truncated UTF-8 fail closed", async () => {
    await assert.rejects(consumeSse(stream(utf8.encode("data:" + "x".repeat(131073)), 200000), () => {}));
    await assert.rejects(consumeSse(stream(new Uint8Array([0xe4, 0xb8])), () => {}));
});
