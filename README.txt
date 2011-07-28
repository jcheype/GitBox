A DropBox clone using Git (internally JGit)

execution:
    java -jar target/gitbox-1.0-SNAPSHOT.one-jar.jar -r "git@github.com:jcheype/repo_gitbox.git" -n "http://www.jcheype.com:8123/"  /Users/mush/tmp/plop
    
help:
    usage: git-box.jar [OPTION]... REPO_FOLDER
     -c,--clone                set repository to clone
     -h,--help                 print this help
     -n,--notification <arg>   set notification server url
     -r,--remote <arg>         remote repository url