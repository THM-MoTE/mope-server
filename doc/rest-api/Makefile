TEXFILE = ./rest-api.tex
#BIBTEXINPUT = ./report.aux
PDFFILE = ./rest-api.pdf
#ZIPFILE = ../projs.zip

all: compile

bib: compile
	bibtex $(BIBTEXINPUT)
	make compile
	make compile

compile:
	pdflatex $(TEXFILE)
	pdflatex $(TEXFILE)

display: compile
	open -a Preview $(PDFFILE)

zip:
	rm $(ZIPFILE)
	zip -r $(ZIPFILE) ./
