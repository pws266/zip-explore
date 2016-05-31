# zip-explore
Task with *.zip and *.gzip archives processing according to Java Basics course

Folders structure:
- files - folder for source *.zip archive. The resulting archive also stores in
          this folder if application executes via "ant run";
- zip-queue - folder for application code and building scripts:
  - ant - scripts for ant building tool.
          Usage: "ant clean" - cleans temporary content obtained via last build;
                 "ant compile" - compiles source files into *.class - files;
                 "ant makejar" - packs *.class file into executable *.jar
                                 archive;
                 "ant run" - executes application stored in *.jar archive. The
                             resulting *.zip will be placed in "files" folder;
  - src - folder with source *.java files;
  - .idea - folder with Intellij IDEA project content;
  - zip-queue.iml - Intellij IDEA project file.

