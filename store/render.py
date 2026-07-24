from PIL import Image, ImageDraw, ImageFont

TEAL  = (0x0E, 0x5C, 0x55)
DEEP  = (0x08, 0x3D, 0x38)
PAPER = (0xF7, 0xF4, 0xEE)
CARD  = (0xFF, 0xFD, 0xF8)
AMBER = (0xB5, 0x55, 0x1F)
SOFT  = (0x55, 0x61, 0x5E)
MUTED = (0xAE, 0xCB, 0xC6)

G = "/usr/share/fonts/truetype/google-fonts/"
f = lambda w, s: ImageFont.truetype(G + "Poppins-" + w + ".ttf", s)

def text(d, xy, s, font, fill, anchor="la"):
    d.text(xy, s, font=font, fill=fill, anchor=anchor)

def width(d, s, font):
    return d.textbbox((0, 0), s, font=font)[2]

def calculator(d, ox, oy, k, body=PAPER):
    """Drawn from the same coordinates as the vector drawable, so the store
    assets and the installed launcher icon cannot drift apart."""
    S = lambda v: v * k
    d.rounded_rectangle([ox+S(30), oy+S(34), ox+S(78), oy+S(74)], radius=S(4), fill=body)
    d.rectangle([ox+S(36), oy+S(40), ox+S(72), oy+S(50)], fill=TEAL)
    for x0, y0, x1, y1 in [(36,56,46,62), (52,56,62,62), (36,66,46,70),
                           (52,66,72,70), (68,56,72,62)]:
        d.rectangle([ox+S(x0), oy+S(y0), ox+S(x1), oy+S(y1)], fill=AMBER)

# ---------------- 512 x 512 store icon ----------------
# Delivered square and flat: Play applies its own mask, rounding and shadow, so
# baking any of that in here would double it up.
ico = Image.new("RGB", (512, 512), TEAL)
calculator(ImageDraw.Draw(ico), 0, 0, 512 / 108)
ico.save("icon-512.png")

# ---------------- 1024 x 500 feature graphic ----------------
# Leads with the finding the app exists to produce rather than with the word
# "calculator". Amber appears exactly once, on the number carrying that finding,
# which is the same rule the app itself follows.
W, H = 1024, 500
g = Image.new("RGB", (W, H), TEAL)
d = ImageDraw.Draw(g)
d.rectangle([0, H-7, W, H], fill=DEEP)

# Right zone: the reveal card, placed first so the left zone can be fitted to
# whatever room is left rather than the other way round.
cw, ch = 322, 216
cx, cy = W - cw - 58, (H - ch) // 2 - 4
d.rounded_rectangle([cx, cy, cx+cw, cy+ch], radius=22, fill=CARD)
text(d, (cx+28, cy+26), "THEY QUOTED", f("Medium", 15), SOFT)
text(d, (cx+26, cy+45), "8% flat", f("Medium", 40), SOFT)
d.line([cx+28, cy+108, cx+cw-28, cy+108], fill=(0xD6, 0xCE, 0xBE), width=2)
text(d, (cx+28, cy+122), "YOU ACTUALLY PAY", f("Medium", 15), AMBER)
text(d, (cx+26, cy+141), "14.13%", f("Bold", 54), AMBER)

# Left zone.
left, right_edge = 58, cx - 48
k = 2.0
icon_w = 48 * k
calculator(d, left - 30 * k, 96, k)

title_f = f("Bold", 52)
while width(d, "MoneyClarity Calc", title_f) > (right_edge - left) and title_f.size > 30:
    title_f = f("Bold", title_f.size - 2)
text(d, (left - 3, 232), "MoneyClarity Calc", title_f, PAPER)

tag_f = f("Regular", 26)
text(d, (left, 300), "The rate you are actually paying", tag_f, MUTED)
text(d, (left, 372), "No permissions.  No account.  No internet.", f("Medium", 22), MUTED)

g.save("feature-graphic-1024x500.png")
print("title width", width(d, "MoneyClarity Calc", title_f), "available", right_edge - left)
print("card x", cx, "left zone ends", right_edge)
