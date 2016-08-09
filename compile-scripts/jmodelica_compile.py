import pymodelica
import argparse

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-classname", nargs=1)
    parser.add_argument("-outputdir", nargs=1)
    parser.add_argument("-file", nargs="+")

    args = parser.parse_args()
    files = args.file[1:]
    className = args.classname
    print "files ", files
    print "classname ", className
    print "output ", parser.outputdir
    #pymodelica.compile_fmu(className, files)

main()
