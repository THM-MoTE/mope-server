TEXFILE = ./protocol.tex
#BIBTEXINPUT = ./report.aux
PDFFILE = ./protocol.pdf

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

clean:
	rm *.aux
	rm *.log
	rm *.toc
	rm *.out
	rm $(PDFFILE)

html:
	asciidoctor protocol.adoc
