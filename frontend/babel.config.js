module.exports = function (api) {
  api.cache(true);
  return {
    presets: [['babel-preset-expo', { jsxImportSource: 'nativewind' }], 'nativewind/babel'],
    // react-native-reanimated/plugin precisa ser sempre o último da lista.
    plugins: ['react-native-reanimated/plugin'],
  };
};
