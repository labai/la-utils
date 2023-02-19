# la-mapper-demo1-singlejar

An example of using runtime compile for single-jar built with `maven assembly plugin`.
In this example the classpath provided using a `kotlin.script.classpath` parameter. 
Also require to add dependencies `kotlin-compiler`, `kotlin-script-util`, `kotlin-scripting-compiler` in pom.xml.

This solution doesn't work with a spring-boot single-jar.

### Run
- execute `mvn package`
- go to `out` dir, execute `run.bat`
- follow messages, there should be successful compile message _Compiled mapper from 'lamapper.demo1.Source' to 'lamapper.demo1.Target'_
