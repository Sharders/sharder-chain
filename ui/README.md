# COS Client UI #

## Compile and run in npm
0. run `npm i`
1. run `npm run generate_theme`
2. run `npm run dev`
3. access URL: [http://localhost:4000](http://localhost:4000)

## Build and run in Client
1. run `npm i`
2. run `npm run generate_theme`
3. run `npm run build`
4. startup cos client service
5. access URL: [http://localhost:8215](http://localhost:8215)

## UI Client Options
Sharder Client:
```properties
ui/src/styles/css/vars.scss
$projectName: sharder

ui/build/config.js
module.exports.title="Sharder-Client"

ui/static/favicon.ico 
ui/static/img/*
> Replace with the corresponding icon
```
After the configuration is complete, the UI will be automatically switched
