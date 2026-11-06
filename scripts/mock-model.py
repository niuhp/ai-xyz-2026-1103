#!/usr/bin/env python3
"""Local OpenAI-compatible fault simulator. Never forwards requests."""
import argparse
import json
import re
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

COUNTS = {}
LOCK = threading.Lock()
DRAFT = {"relevant": True, "draft": {"system": "RuoYi-Vue", "module": "字典管理",
    "category": "DATA_DUPLICATION", "summary": "重复字典值",
    "actualResult": "同一字典值重复添加成功", "expectedResult": "提示字典值已经存在",
    "occurredAtText": "", "version": "3.8.9"}}

class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def do_POST(self):
        payload = json.loads(self.rfile.read(int(self.headers.get('Content-Length', '0'))))
        content = payload.get('messages', [{}])[-1].get('content', '')
        match = re.search(r'\[demo:(\w+):([\w-]+)\]', content)
        scenario, request = match.groups() if match else ('success', 'default')
        with LOCK:
            COUNTS[request] = COUNTS.get(request, 0) + 1
            attempt = COUNTS[request]
        status, code, retry_after = 200, '', None
        if scenario == 'timeout':
            time.sleep(2.5)
        elif scenario == 'rate_limit':
            status, code = 429, 'rate_limit_exceeded'
        elif scenario == 'auth':
            status, code = 401, 'invalid_api_key'
        elif scenario == 'quota':
            status, code = 429, 'insufficient_quota'
        elif scenario == 'unavailable' or (scenario == 'recover' and attempt == 1):
            status, code = 503, 'service_unavailable'
        elif scenario == 'retry_after':
            status, code, retry_after = 503, 'service_unavailable', '10'
        print(json.dumps({'scenario': scenario, 'request': request, 'attempt': attempt,
                          'status': status}), flush=True)
        if status == 200:
            body = {'id': 'chatcmpl-local-demo', 'object': 'chat.completion', 'created': 1,
                    'model': 'local-fault-simulator',
                    'choices': [{'index': 0, 'finish_reason': 'stop',
                                 'message': {'role': 'assistant', 'content': json.dumps(DRAFT, ensure_ascii=False)}}],
                    'usage': {'prompt_tokens': 0, 'completion_tokens': 0, 'total_tokens': 0}}
        else:
            body = {'error': {'message': 'local simulated failure', 'code': code,
                              'type': code, 'param': None}}
        data = json.dumps(body, ensure_ascii=False).encode()
        try:
            self.send_response(status)
            self.send_header('Content-Type', 'application/json')
            if retry_after:
                self.send_header('Retry-After', retry_after)
            self.send_header('Content-Length', str(len(data)))
            self.end_headers()
            self.wfile.write(data)
        except (BrokenPipeError, ConnectionResetError):
            pass

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--port', type=int, default=18083)
    args = parser.parse_args()
    ThreadingHTTPServer(('127.0.0.1', args.port), Handler).serve_forever()
