within moTests2.functions;
model fib
  Real n=1;
equation
  der(n) = n+1;
end fib;
