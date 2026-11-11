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
        task = None
        try:
            task = json.loads(content)
        except (ValueError, TypeError):
            pass
        if isinstance(task, dict) and task.get('task') == 'knowledge-advice':
            content = task['description']
        else:
            task = None
        match = re.search(r'\[demo:(\w+):([\w-]+)\]', content)
        scenario, request = match.groups() if match else ('success', 'default')
        with LOCK:
            COUNTS[request] = COUNTS.get(request, 0) + 1
            attempt = COUNTS[request]
        status, code, retry_after = 200, '', None
        if scenario == 'timeout':
            time.sleep(15 if payload.get('stream') else 2.5)
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
        if status == 200 and payload.get('stream'):
            self.stream(payload, scenario)
            return
        if status == 200:
            output = self.advice(task, scenario) if task else DRAFT
            body = {'id': 'chatcmpl-local-demo', 'object': 'chat.completion', 'created': 1,
                    'model': 'local-fault-simulator',
                    'choices': [{'index': 0, 'finish_reason': 'stop',
                                 'message': {'role': 'assistant', 'content': json.dumps(output, ensure_ascii=False)}}],
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

    def advice(self, task, scenario):
        if scenario == 'insufficient':
            return {'supported': False, 'summary': '资料不足，暂不提出具体处理建议。', 'steps': [],
                    'limitations': ['请补充发生时间、日志与复现条件。']}
        if task['mode'] == 'BASELINE':
            return {'supported': True, 'summary': '以下是未查阅历史案例的通用排查方向，不能据此确定根因。',
                'steps': [{'text': '先记录发生时间、请求编号和实际报错，在测试环境复现并检查日志。', 'evidence': []},
                          {'text': '比较正常与异常请求的差异，补充范围、数据量及部署版本。', 'evidence': []}],
                'limitations': ['本回答没有历史案例依据，未执行检查，也未确定根因。']}
        steps = []
        for doc in task['documents'][:2]:
            steps.append({'text': '参考教学案例，先人工核对适用条件，再考虑：' + doc['resolution'],
                          'evidence': [{'caseId': doc['id'], 'quote': doc['resolution']}]})
        if scenario == 'bad_reference':
            steps[0]['evidence'][0]['caseId'] = 'KB-999'
        elif scenario == 'bad_quote':
            steps[0]['evidence'][0]['quote'] = '这是一段不存在于原始案例的虚构处理记录。'
        return {'supported': True, 'summary': '已找到词面相关教学案例，以下只作为排查参考，不代表当前根因。',
                'steps': steps, 'limitations': ['资料均为教学模拟，请核对实际版本与症状。',
                                              '引用摘录匹配不等于建议一定适用，不自动执行任何操作。']}

    def stream(self, payload, scenario):
        """Send real upstream chunks; the browser does not fake a typing effect."""
        content = payload.get('messages', [{}])[-1].get('content', '')
        draft = DRAFT
        if '昨天下午开始导出失败' in content:
            draft = {'relevant': True, 'draft': dict(DRAFT['draft'], system='', module='导出',
                category='EXPORT', summary='导出超时', actualResult='导出失败，提示超时',
                expectedResult='', occurredAtText='昨天下午', version='')}
        elif '写一首' in content:
            draft = {'relevant': False, 'draft': None}
        text = json.dumps(draft, ensure_ascii=False)
        if scenario == 'invalid':
            text = '模型没有返回 JSON'
        try:
            self.send_response(200)
            self.send_header('Content-Type', 'text/event-stream')
            self.send_header('Cache-Control', 'no-cache')
            self.end_headers()
            time.sleep(0.5)
            for offset in range(0, len(text), 18):
                self.chunk({'content': text[offset:offset + 18]})
                if scenario == 'disconnect' and offset >= 36:
                    self.close_connection = True
                    return
                time.sleep(0.16)
            if scenario != 'missing_finish':
                self.chunk({}, 'length' if scenario == 'truncated' else 'stop')
                self.write_event({'id': 'chatcmpl-local-demo', 'object': 'chat.completion.chunk',
                    'created': 1, 'model': 'local-fault-simulator', 'choices': [],
                    'usage': {'prompt_tokens': 0, 'completion_tokens': 0, 'total_tokens': 0}})
            self.wfile.write(b'data: [DONE]\n\n')
            self.wfile.flush()
        except (BrokenPipeError, ConnectionResetError):
            pass
        finally:
            self.close_connection = True

    def chunk(self, delta, reason=None):
        self.write_event({'id': 'chatcmpl-local-demo', 'object': 'chat.completion.chunk',
            'created': 1, 'model': 'local-fault-simulator',
            'choices': [{'index': 0, 'delta': delta, 'finish_reason': reason}]})

    def write_event(self, event):
        self.wfile.write(('data: ' + json.dumps(event, ensure_ascii=False) + '\n\n').encode())
        self.wfile.flush()

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--port', type=int, default=18083)
    args = parser.parse_args()
    ThreadingHTTPServer(('127.0.0.1', args.port), Handler).serve_forever()
