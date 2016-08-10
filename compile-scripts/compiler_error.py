import json

class CompilerError:
    def __init__(self, tpe, file, startLine, startCol, endLine, endCol, msg):
        self.tpe = tpe
        self.file = file
        self.start = FilePosition(startLine, startCol)
        self.end  = FilePosition(endLine, endCol)
        self.msg = msg
    def __init__(self, tpe, file, startLine, startCol, msg):
        self.tpe = tpe
        self.file = file
        self.start = FilePosition(startLine, startCol)
        self.end  = FilePosition(startLine, startCol)
        self.msg = msg

    def to_JSON(self):
        dic = {
            "type": self.tpe,
            "file": self.file,
            "start": self.start.to_dict(),
            "end": self.end.to_dict(),
            "message": self.msg
        }
        return json.dumps(dic, indent=2, sort_keys=True)

class FilePosition:
    def __init__(self, line, column):
        self.line = line
        self.column = column

    def to_dict(self):
        return {
            "line": self.line,
            "column": self.column
        }
