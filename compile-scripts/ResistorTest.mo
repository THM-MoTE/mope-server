odel ResistorTest
  Modelica.Electrica.Analog.Basic.Ground ground1 annotation(Placement(visible = true, transformation(origin = {-56, 14}, extent = {{-10, -10}, {10, 10}}, rotation = 0)));
  Modelica.Electrical.Analog.Basic.Resistor resistor1 annotation(Placement(visible = true, transformation(origin = {-12, 56}, extent = {{-10, -10}, {10, 10}}, rotation = 0)));
  Modelica.Electrical.Analog.Basic.Resistor resistor2 annotation(Placement(visible = true, transformation(origin = {28, 34}, extent = {{-10, -10}, {10, 10}}, rotation = -90)));
  fibs f;
  //test2 t;
equation
  connect(resistor1.n, ground1.p) annotation(Line(points = {{28, 24}, {-56, 24}}, color = {0, 0, 255}));
  connect(resistor1.n, resistor2.p) annotation(Line(points = {{-2, 56}, {28, 56}, {28, 44}}, color = {0, 0, 255}));
  connect(ground1.p, resistor1.p) annotation(Line(points = {{-56, 24}, {-22, 24}, {-22, 56}}, color = {0, 0, 255}));
annotation(    
  Icon (
    coordinateSystem(
      extent = {{0,0},{300,300}}
    ),
    graphics = {
      Line(
        origin = {80,229},
        points = {{-30,27},{30,-27}},
        color = {0,0,0},
        pattern = LinePattern.Solid,
        thickness = 4.0
      ),
      Polygon(
        origin = {74,83},
        points = {{-17,43},{4,5},{30,16},{26,-43},{-30,3}},
        lineColor = {0,0,0},
        fillColor = {255,0,0},
        lineThickness = 4.0,
        pattern = LinePattern.Solid,
        fillPattern = FillPattern.Solid
      ),
      Rectangle(
        origin = {225,200},
        lineColor = {0,0,0},
        fillColor = {255,0,0},
        lineThickness = 4.0,
        pattern = LinePattern.Solid,
        fillPattern = FillPattern.Solid,
        extent = {{-69,52}, {69,-52}}
      ),
      Ellipse(
        origin = {209,77},
        lineColor = {0,0,0},
        fillColor = {255,0,0},
        lineThickness = 4.0,
        pattern = LinePattern.Solid,
        fillPattern = FillPattern.Solid,
        extent = {{-55,36}, {55,-36}},
        endAngle = 360
      )
    }));
end ResistorTest;
