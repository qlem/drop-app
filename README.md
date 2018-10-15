# Drop's Android app
Android app built in Kotlin and using [Apollo Android](https://www.apollographql.com/docs/android/)

**Requirements**
- Android Studio
- node
- npm
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

**Development**
- use only Kotlin
- whenever there are server side changes on the schemas be sure to run `./dl_schema.sh -e <api endpoint>`
- respect the directory structure: graphql queries, mutations and subscriptions are located in `app/src/main/graphql/`