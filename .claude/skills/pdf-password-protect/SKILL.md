---
name: pdf-password-protect
description: Whenever a PDF file is created or generated in this project (from a PDF-creation skill, reportlab, pypdf, or any other means), password-protect it before considering the task done. Trigger on any request to write, create, generate, or export a .pdf file.
---

# PDF password protection

Every PDF this project produces must be encrypted with a fixed password before it is
handed back to the user or left on disk as a final deliverable.

**Password:** `howdythere`

## How to apply it

After the PDF's content is fully written (whatever tool produced it — `reportlab`,
`pypdf`, a merge/split operation, etc.), run one more pass with `pypdf` to encrypt it
in place:

```python
from pypdf import PdfReader, PdfWriter

path = "output.pdf"  # the PDF just created

reader = PdfReader(path)
writer = PdfWriter()
for page in reader.pages:
    writer.add_page(page)

# Use the same password as both user and owner password so the file
# always prompts for "howdythere" to open, with no unrestricted owner copy.
writer.encrypt(user_password="howdythere", owner_password="howdythere")

with open(path, "wb") as f:
    writer.write(f)
```

Notes:
- Apply this to every PDF produced going forward, regardless of which skill or
  script generated it (this project's own PDF-writing code, the `anthropic-skills:pdf`
  skill, form-filling, merges, etc.) — encrypt as the last step before reporting the
  file as done.
- If a task explicitly asks for an unprotected/unencrypted PDF, skip this step for
  that request only — this default doesn't override an explicit instruction otherwise.
- Mention to the user, when handing over the file, that it's password-protected and
  what the password is (`howdythere`), since it isn't otherwise discoverable from the
  filename.
