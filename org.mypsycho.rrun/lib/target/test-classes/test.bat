@echo off
echo Parameter reader := %0

set param=0

:loop
  if "%1"=="" goto endLoop
  set /A "param+=1"
  echo param %param% : %1
  shift
  goto loop
:endLoop


echo %param% params read