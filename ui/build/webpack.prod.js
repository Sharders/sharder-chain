"use strict";
process.env.NODE_ENV = "production";

const exec = require("child_process").execSync;
const webpack = require("webpack");
const ExtractTextPlugin = require("extract-text-webpack-plugin");
const OptimizeCssAssetsPlugin = require("optimize-css-assets-webpack-plugin");
const PreloadWebpackPlugin = require("preload-webpack-plugin");
const ProgressPlugin = require("webpack/lib/ProgressPlugin");
const OfflinePlugin = require("offline-plugin");
const base = require("./webpack.base");
const config = require("./config");

// remove dist folder
// exec('rm -rf dist/')
// use source-map
base.devtool = "cheap-source-map";
base.module.rules.push(
  {
    test: /\.css$/,
    use: ExtractTextPlugin.extract({
      fallback: "style-loader",
      use: "css-loader"
    })
  },
  {
    test: /\.scss$/,
    use: ExtractTextPlugin.extract({
      fallback: "style-loader",
      use: ["css-loader", "sass-loader"]
    })
  }
);
// a white list to add dependencies to vendor chunk
base.entry.vendor = config.vendor;
// use hash filename to support long-term caching
base.output.filename = "[name].[chunkhash:8].js";
// add webpack plugins
base.plugins.push(
  new ProgressPlugin(),
  new ExtractTextPlugin("[name].[contenthash:8].css"),
  new OptimizeCssAssetsPlugin(),
  new webpack.DefinePlugin({
    "process.env.NODE_ENV": JSON.stringify("production")
  }),
  new webpack.optimize.UglifyJsPlugin({
    sourceMap: false,
    compress: {
      warnings: false,
      drop_console: true,
      pure_funcs: ['console.log']
    },
    output: {
      comments: false
    }
  }),
  // extract vendor chunks
  new webpack.optimize.CommonsChunkPlugin({
    name: "vendor",
    filename: "vendor.[chunkhash:8].js"
  }),
  new PreloadWebpackPlugin({ rel: "preload", as: "script", include: "all" }),
  // progressive web app
  // it uses the publicPath in webpack config
  new OfflinePlugin({
    relativePaths: false,
    AppCache: false,
    ServiceWorker: {
      events: true
    }
  })
);

// minimize webpack output
base.stats = {
  // Add children information
  children: false,
  // Add chunk information (setting this to `false` allows for a less verbose output)
  chunks: false,
  // Add built modules information to chunk information
  chunkModules: false,
  chunkOrigins: false,
  modules: false
};

module.exports = base;
