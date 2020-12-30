"use strict";
const path = require("path");
const webpack = require("webpack");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const CopyWebpackPlugin = require("copy-webpack-plugin");
const config = require("./config");
const _ = require("./utils");

module.exports = {
  entry: {
    src: "./src/index.js"
  },
  output: {
    path: _.outputPath,
    filename: "[name].js",
    publicPath: config.publicPath
  },
  performance: {
    hints: process.env.NODE_ENV === "production" ? "warning" : false
  },
  resolve: {
    extensions: [
      ".js",
      ".vue",
      ".css",
      ".json",
      ".scss",
      ".eot",
      ".svg",
      ".ttf",
      ".woff"
    ],
    alias: {
      root: path.join(__dirname, "../src"),
      components: path.join(__dirname, "../src/components"),
      views: path.join(__dirname, "../src/views"),
      router: path.join(__dirname, "../src/router"),
        'vue$': 'vue/dist/vue.esm.js',
        theme: path.join(__dirname, "../theme"), // get Element-UI icons
      scss_vars: path.resolve(__dirname, "../src/styles/css/vars.scss"), //  get scss vars
      styles: path.join(__dirname, "../src/styles") // get scss files

    },
    modules: [
      // places where to search for required modules
      _.cwd("node_modules"),
      _.cwd("src"),
      _.cwd("theme")
    ]
  },
  module: {
    rules: [
      {
        test: /\.vue$/,
        use: "vue-loader"
      },
      // {
      //   test: /\.js$/,
      //   enforce: "pre",
      //   use: "eslint-loader?fix=true",
      //   exclude: [/node_modules/]
      // },
      {
        test: /\.js$/,
        use: "babel-loader",
        exclude: [/node_modules/]
      },
      {
        test: /\.(ico|eot|otf|webp|ttf|woff|woff2)(\?.*)?$/,
        use: "file-loader?limit=100000"
      },
      {
        test: /\.(jpe?g|png|gif|svg)$/i,
        use: [
          "file-loader?limit=100000",
          {
            loader: "img-loader",
            options: {
              enabled: true,
              optipng: true
            }
          }
        ]
      }
    ]
  },
  plugins: [
    new HtmlWebpackPlugin({
      title: config.title,
      template: path.resolve(__dirname, "index.html"),
      filename: _.outputIndexPath
    }),
    new CopyWebpackPlugin([
      {
        from: _.cwd("./static"),
        // to the root of dist path
        to: "./"
      }
    ]),
      // new webpack.optimize.CommonsChunkPlugin('common.js'),
      new webpack.ProvidePlugin({
          jQuery: "jquery",
          $: "jquery"
      })
  ],
  target: _.target
};
