/** Incremental UTF-8 / SSE parser. EOF is not an application success event. */
export async function consumeSse(stream, onEvent) {
    if (!stream) throw new Error("响应体为空");
    const reader = stream.getReader();
    const decoder = new TextDecoder("utf-8", {fatal: true});
    let buffer = "";
    let stopped = false;
    const dispatch = frame => {
        let event = "message";
        const data = [];
        for (const line of frame.split(/\r?\n/)) {
            if (!line || line.startsWith(":")) continue;
            const colon = line.indexOf(":");
            const name = colon < 0 ? line : line.slice(0, colon);
            let value = colon < 0 ? "" : line.slice(colon + 1);
            if (value.startsWith(" ")) value = value.slice(1);
            if (name === "event") event = value;
            if (name === "data") data.push(value);
        }
        if (data.length) stopped = onEvent(event, data.join("\n")) === false;
    };
    const drain = () => {
        let match;
        while (!stopped && (match = /\r?\n\r?\n/.exec(buffer))) {
            dispatch(buffer.slice(0, match.index));
            buffer = buffer.slice(match.index + match[0].length);
        }
        if (buffer.length > 131072) throw new Error("SSE 帧过长");
    };
    try {
        while (!stopped) {
            const {value, done} = await reader.read();
            buffer += done ? decoder.decode() : decoder.decode(value, {stream: true});
            drain();
            if (done) break;
        }
        // 没有空行结束的残帧直接丢弃，由调用方检查是否收到 result/failure。
    } finally {
        try { await reader.cancel(); } finally { reader.releaseLock(); }
    }
}
