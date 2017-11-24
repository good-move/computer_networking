const path = require("path");
const ExtractTextPlugin = require("extract-text-webpack-plugin");

module.exports = {
    entry: path.resolve(__dirname, "src/index.js"),
    output: {
        path: path.resolve(__dirname, "dist"),
        filename: "bundle.js"
    },
    module: {
        rules: [
            {
                enforce: "pre",
                test: /\.(css|sass|scss)$/,
                exclude: path.resolve(__dirname, "node_modules"),
                loaders: ExtractTextPlugin.extract([
                    {
                        loader: "css-loader",
                        options: {
                            modules: true,
                            localIdentName: "[path][name]__[local]"
                        }
                    }, "sass-loader"
                ])
            },
            {
                test: /\.jsx?$/,
                exclude: path.resolve(__dirname, "node_modules"),
                loader: "babel-loader"
            }
        ]
    },
    plugins: [
        new ExtractTextPlugin({
            filename: "bundle.css",
            allChunks: true
        })
    ]
};
