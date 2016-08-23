# Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

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

    def to_dict(self):
        return {
            "type": self.tpe,
            "file": self.file,
            "start": self.start.to_dict(),
            "end": self.end.to_dict(),
            "message": self.msg
        }
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
