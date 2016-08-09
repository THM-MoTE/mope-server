import pymodelica
import argparse

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("file", nargs="+")
    parser.add_argument("classname", nargs=1)

    args = parser.parse_args()
    files = args.file[1:]
    className = args.classname
    print "files ", files
    print "classname ", className
    #pymodelica.compile_fmu(className, files)

main()
