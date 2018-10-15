# Drop's Android app
Android app built in Kotlin and using [Apollo Android](https://www.apollographql.com/docs/android/)

**Requirements**
- Android Studio
- node
- npm
- [fish](https://fishshell.com/)
- [Apollo CLI](https://github.com/apollographql/apollo-cli)
```
$ npm i -g apollo
```

**Installation**
```
$ git clone ssh://git@phabricator.drop.run/source/app_android.git
$ cd app_android
$ ./dl_schema.fish
```

**Development**
- use only Kotlin
- whenever there are server side changes on the schemas be sure to run `$ ./dl_schema.fish` and recompile