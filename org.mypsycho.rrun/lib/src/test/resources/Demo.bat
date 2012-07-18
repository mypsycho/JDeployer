
@ECHO OFF

CALL ..\..\environment.bat
SET CLASSPATH=Deployer.jar


%JAVA% -classpath %CLASSPATH% com.psycho.deploy.Processor
%JAVA% -classpath %CLASSPATH% com.psycho.deploy.Processor 6001
%JAVA% -classpath %CLASSPATH% com.psycho.deploy.Controller