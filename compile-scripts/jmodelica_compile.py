#import pymodelica
import argparse
import os.path as path

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-classname", nargs=1)
    parser.add_argument("-outputdir", nargs=1)
    parser.add_argument("-file", nargs="+")

    args = parser.parse_args()
    files = [x for x in args.file if path.exists(x)]
    className = args.classname
    print "files ", files
    print "classname ", className
    print "output ", args.outputdir
    #pymodelica.compile_fmu(className, files)

main()
