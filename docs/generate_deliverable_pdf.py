from pathlib import Path

from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.platypus import Paragraph, Preformatted, SimpleDocTemplate, Spacer


BASE_DIR = Path(__file__).resolve().parent
SOURCE = BASE_DIR / "NovaBank_Modulo_3_Entregable.md"
TARGET = BASE_DIR / "NovaBank_Modulo_3_Entregable.pdf"


def paragraph(text: str, style_name: str, styles):
    escaped = (
        text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("`", "")
    )
    return Paragraph(escaped, styles[style_name])


def main():
    styles = getSampleStyleSheet()
    document = SimpleDocTemplate(
        str(TARGET),
        pagesize=A4,
        rightMargin=2 * cm,
        leftMargin=2 * cm,
        topMargin=1.8 * cm,
        bottomMargin=1.8 * cm,
        title="NovaBank Digital Services - Modulo 3",
    )

    story = []
    in_code = False
    code_lines = []

    for raw_line in SOURCE.read_text(encoding="utf-8").splitlines():
        line = raw_line.rstrip()

        if line.startswith("```"):
            if in_code:
                story.append(Preformatted("\n".join(code_lines), styles["Code"]))
                story.append(Spacer(1, 0.2 * cm))
                code_lines = []
                in_code = False
            else:
                in_code = True
            continue

        if in_code:
            code_lines.append(line)
            continue

        if not line:
            story.append(Spacer(1, 0.12 * cm))
            continue

        if line.startswith("# "):
            story.append(paragraph(line[2:], "Title", styles))
            story.append(Spacer(1, 0.25 * cm))
        elif line.startswith("## "):
            story.append(paragraph(line[3:], "Heading2", styles))
            story.append(Spacer(1, 0.12 * cm))
        elif line.startswith("- "):
            story.append(paragraph("&#8226; " + line[2:], "BodyText", styles))
        else:
            story.append(paragraph(line, "BodyText", styles))

    document.build(story)
    print(TARGET)


if __name__ == "__main__":
    main()
