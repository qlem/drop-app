# Drop's Android app
Android app built in Kotlin and using [Apollo Android](https://www.apollographql.com/docs/android/)

**Requirements**
- Android Studio
- node
- npm
- php
- [Apollo CLI](https://github.com/apollographql/apollo-cli)
```
$ npm i -g apollo
```

**Installation**
```
$ git clone ssh://git@phabricator.drop.run/source/app_android.git
$ cd app_android
```
For the next step, replace `<api endpoint>` with the endpoint of the API eg http://localhost:4000 when the API is running locally
```
$ ./dl_schema.sh -e <api endpoint>
```

**Arcanist**

[Arcanist](https://secure.phabricator.com/book/phabricator/article/arcanist/) is the revision tool used for the project (the Github's PR system equivalent)

In a directory of your choice (like /opt) enter the following commands
```
$ git clone https://github.com/phacility/libphutil.git
$ git clone https://github.com/phacility/arcanist.git
```
then add `/path/to/arcanist/bin/` to your PATH environment variable to make `arc` command available from anywhere.
Finally you can add the bellow instruction in your shell config file (eg. `.bashrc`)
```
source /path/to/arcanist/resources/shell/bash-completion
```

**Push changes and review workflow**

First for each new feature/fix/task you have to create a new feature branch from `master`. Then you commit and push on it.
Once your work is done on this branch you need to send it to review, ie. you create a revision:
```
$ arc diff
```
This will automatically open a new Revision on [phabricator](https://phabricator.drop.run/differential/).
Then you have to wait for reviewers to check it and validate it.
If you need to update your revision (eg. apply some corrections), commit your changes and run
```
$ arc diff --update <revision_title>
```
When the Revision is accepted you need to merge your branch in `master`
```
$ arc land <feature_branch>
```

**Development**
- never work directly on `master` branch!
- for each new feature/tasks/change/fix whatever it is, you have to create a new branch from `master` and work on it!
- use only Kotlin
- whenever there are server side changes on the schemas be sure to run `./dl_schema.sh -e <api endpoint>`
- respect the directory structure: graphql queries, mutations and subscriptions are located in `app/src/main/graphql/`

**Update Arcanist**
```
$ arc upgrade
```