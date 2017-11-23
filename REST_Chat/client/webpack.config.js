const path = require("path");
const ExtractTextPlugin = require("extract-text-webpack-plugin");

module.exports = {
    entry: path.resolve(__dirname, "src/index.js"),
    context: path.resolve(__dirname, "dist"),
    output: {
        filename: "bundle.js"
    },
    module: {
        rules: [
            {
                enforce: "pre",
                test: /.(css|sass|scss)$/,
                exclude: /node_modules/,
                loader: ExtractTextPlugin.extract(["css-loader", "sass-loader"])
            },
            {
                test: /.jsx?$/,
                exclude: /node_modules/,
                loader: "babel-loader"
            }
        ]
    },
    plugins: [
        new ExtractTextPlugin({
            filename: "bundle.css",
            allChunks: true
        })
    ],
    watch: true
};
