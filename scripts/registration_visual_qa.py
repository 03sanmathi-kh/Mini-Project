from pathlib import Path
from PIL import Image,ImageOps,ImageDraw
import pypdfium2 as pdfium
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'FINAL_GATE3_DELIVERABLES/private-build'
exports=Path(r'C:/Users/thele/AppData/Local/Packages/OpenAI.Codex_2p2nqsd0c76g0/LocalCache/Roaming/Codex/web/Codex/artifact-sessions/exports/latest/e2f9c42a67bff0560dec581cab670e224d625e668181761af86ed50c0ad305b9')
def montage(paths,destination,width=600,columns=2):
 images=[]
 for p in paths:
  im=Image.open(p).convert('RGB');im.thumbnail((width,600));tile=Image.new('RGB',(width,im.height+30),'white');tile.paste(im,((width-im.width)//2,30));ImageDraw.Draw(tile).text((8,8),p.stem,fill='black');images.append(tile)
 rowheight=max(i.height for i in images);canvas=Image.new('RGB',(width*columns,rowheight*((len(images)+columns-1)//columns)), '#bbbbbb')
 for n,im in enumerate(images):canvas.paste(im,((n%columns)*width,(n//columns)*rowheight))
 canvas.save(destination)
slides=[exports/f'slide-{i}.png' for i in [5,6,13,22,23,24,25,26,27,28]]
montage(slides,OUT/'slides-registration-qa.png')
pdf=pdfium.PdfDocument(OUT/'handbook-registration.pdf');paths=[]
for n in range(len(pdf)):
 p=OUT/f'handbook-page-{n+1:02}.png';pdf[n].render(scale=1.25).to_pil().save(p);paths.append(p)
for start in range(0,len(paths),8):montage(paths[start:start+8],OUT/f'handbook-montage-{start//8+1}.png',width=450,columns=2)
print('Rendered handbook pages:',len(pdf));print('Slide and handbook QA montages created.')
