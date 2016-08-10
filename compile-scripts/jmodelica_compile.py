from pymodelica.compiler_wrappers import ModelicaCompiler
import argparse
import os.path as path
import re
from compiler_error import *

warningRegex = "Warning: in file '([^']+)':\n\w+ line (\d+), column (\d+):\n\s*(.*)"
errorRegex = "Error: in file '([^']+)':\n\w+ error at line (\d+), column (\d+):\n\s*(.*)"
warningPattern = re.compile(warningRegex)
errorPattern = re.compile(errorRegex)

def __convert_to(matchObj, tpe):
    file, line, col, msg = matchObj
    return CompilerError(tpe, file, line, col, msg)

def convert_to_error(matchObj):
    return __convert_to(matchObj, "Error")

def convert_to_warning(matchObj):
    return __convert_to(matchObj, "Warning")

def parse_error(e):
    errorMatchings = errorPattern.findall(str(e))
    warningMatchings = warningPattern.findall(str(e))
    errorObjs = [convert_to_error(x) for x in errorMatchings]
    warningObjs = [convert_to_warning(x) for x in warningMatchings]
    return errorObjs + warningObjs

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-classname", nargs=1, help="Name of the class, which should get instantiated")
    parser.add_argument("-file", nargs="+", help="The Modelica Source files")

    args = parser.parse_args()

    if(args.file is None):
        print "Nothing to compile"
        return
    else:
        files = [x for x in args.file if path.exists(x)]
        className = args.classname[0]
        mc = ModelicaCompiler()
        targetObj = mc.create_target_object("me", "1.0")
        try:
            ast = mc.parse_model(files)
            if(className is not None):
                instance = mc.instantiate_model(ast, className, targetObj)
        except Exception as e:
            errorObj = parse_error(e)
            for x in errorObj:
                print x.to_JSON()

main()
