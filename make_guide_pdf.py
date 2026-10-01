from html.parser import HTMLParser
from pathlib import Path
import re

source = Path('/home/mike/trace-finder/trace-finder-project-guide.html').read_text(encoding='utf-8')

class TextParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.lines = []
        self.current = []
        self.pre = False
    def flush(self):
        text = ''.join(self.current).strip()
        if text:
            self.lines.append(text)
        self.current = []
    def handle_starttag(self, tag, attrs):
        if tag == 'pre':
            self.flush()
            self.pre = True
        elif tag in ('h1', 'h2', 'h3', 'p', 'li'):
            self.flush()
    def handle_endtag(self, tag):
        if tag == 'pre':
            self.flush()
            self.pre = False
            self.lines.append('')
        elif tag in ('h1', 'h2', 'h3', 'p', 'li'):
            self.flush()
    def handle_data(self, data):
        if self.pre:
            for line in data.splitlines():
                self.lines.append(line.rstrip())
        else:
            self.current.append(re.sub(r'\s+', ' ', data))

parser = TextParser()
parser.feed(source)
raw_lines = parser.lines

wrapped = []
for line in raw_lines:
    if not line:
        wrapped.append('')
        continue
    width = 96
    if line.startswith('Tracefinder Project Guide'):
        width = 80
    words = line.split()
    current = ''
    for word in words:
        if current and len(current) + len(word) + 1 > width:
            wrapped.append(current)
            current = word
        else:
            current = word if not current else current + ' ' + word
    if current:
        wrapped.append(current)

pages = []
for index in range(0, len(wrapped), 52):
    pages.append(wrapped[index:index + 52])

objects = []
def add_object(value):
    objects.append(value)
    return len(objects)

font_id = add_object(b'<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>')
page_ids = []
content_ids = []
for page_lines in pages:
    commands = ['BT', '/F1 9 Tf', '42 800 Td', '12 TL']
    for line in page_lines:
        safe = line.replace('\\', '\\\\').replace('(', '\\(').replace(')', '\\)')
        commands.append(f'({safe}) Tj')
        commands.append('0 -1.0 TD')
    commands.append('ET')
    stream = '\n'.join(commands).encode('latin-1', errors='replace')
    content_ids.append(add_object(b'<< /Length ' + str(len(stream)).encode() + b' >>\nstream\n' + stream + b'\nendstream'))

for content_id in content_ids:
    page_ids.append(add_object(b''))

pages_kids = b' '.join(f'{page_id} 0 R'.encode() for page_id in page_ids)
pages_id = add_object(b'<< /Type /Pages /Kids [' + pages_kids + b'] /Count ' + str(len(page_ids)).encode() + b' >>')
for page_id, content_id in zip(page_ids, content_ids):
    objects[page_id - 1] = (b'<< /Type /Page /Parent ' + str(pages_id).encode() +
                             b' 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 ' +
                             str(font_id).encode() + b' 0 R >> >> /Contents ' +
                             str(content_id).encode() + b' 0 R >>')

catalog_id = add_object(b'<< /Type /Catalog /Pages ' + str(pages_id).encode() + b' 0 R >>')
pdf = bytearray(b'%PDF-1.4\n%\xe2\xe3\xcf\xd3\n')
offsets = [0]
for object_number, value in enumerate(objects, start=1):
    offsets.append(len(pdf))
    pdf.extend(f'{object_number} 0 obj\n'.encode())
    pdf.extend(value)
    pdf.extend(b'\nendobj\n')
xref = len(pdf)
pdf.extend(f'xref\n0 {len(objects) + 1}\n'.encode())
pdf.extend(b'0000000000 65535 f \n')
for offset in offsets[1:]:
    pdf.extend(f'{offset:010d} 00000 n \n'.encode())
pdf.extend(f'trailer\n<< /Size {len(objects) + 1} /Root {catalog_id} 0 R >>\nstartxref\n{xref}\n%%EOF\n'.encode())
Path('/home/mike/trace-finder/trace-finder-project-guide.pdf').write_bytes(pdf)
print(f'created {len(pdf)} bytes across {len(pages)} pages')
