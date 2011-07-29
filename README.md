A DropBox clone using Git (internally JGit) : [(GitBox snapshot)](http://commondatastorage.googleapis.com/gitbox/gitbox-1.0-SNAPSHOT.one-jar.jar)

The notification server is a simple HTTP PUSH server based on Netty, you can use the public one:

    http://www.jcheype.com:8123/

execution:

    java -jar target/gitbox-1.0-SNAPSHOT.one-jar.jar -r "git@github.com:jcheype/repo_gitbox.git" -n "http://www.jcheype.com:8123/"  /Users/mush/tmp/plop
    
help:

    usage: git-box.jar [OPTION]... REPO_FOLDER
     -c,--clone                set repository to clone
     -h,--help                 print this help
     -n,--notification <arg>   set notification server url
     -r,--remote <arg>         remote repository url