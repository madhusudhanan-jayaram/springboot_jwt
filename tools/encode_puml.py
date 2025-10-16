import zlib
import os


def encode_plantuml(s: str) -> str:
    # raw DEFLATE (no zlib header)
    compressor = zlib.compressobj(level=9, wbits=-15)
    compressed = compressor.compress(s.encode('utf-8')) + compressor.flush()

    def encode6bit(b: int) -> str:
        if b < 10:
            return chr(48 + b)
        b -= 10
        if b < 26:
            return chr(65 + b)
        b -= 26
        if b < 26:
            return chr(97 + b)
        b -= 26
        if b == 0:
            return '-'
        if b == 1:
            return '_'
        return '?'

    def append3bytes(b1: int, b2: int, b3: int) -> str:
        c1 = (b1 >> 2) & 0x3F
        c2 = ((b1 & 0x3) << 4) | ((b2 >> 4) & 0xF)
        c3 = ((b2 & 0xF) << 2) | ((b3 >> 6) & 0x3)
        c4 = b3 & 0x3F
        return ''.join(encode6bit(c) for c in (c1, c2, c3, c4))

    res = []
    data = compressed
    for i in range(0, len(data), 3):
        b1 = data[i]
        b2 = data[i + 1] if i + 1 < len(data) else 0
        b3 = data[i + 2] if i + 2 < len(data) else 0
        res.append(append3bytes(b1, b2, b3))
    return ''.join(res)


if __name__ == '__main__':
    docs_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'docs', 'sequence.puml'))
    if not os.path.exists(docs_path):
        print(f"ERROR: {docs_path} not found", file=sys.stderr)
        raise SystemExit(1)
    with open(docs_path, 'r', encoding='utf-8') as fh:
        text = fh.read()
    print(encode_plantuml(text))

