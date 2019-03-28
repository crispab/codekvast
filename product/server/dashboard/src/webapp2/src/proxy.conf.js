module.exports = [
  {
    context: [
      "/api-docs",
      "/dashboard",
      "/swagger",
      "/webjars",
    ],
    headers: {
      "Access-Control-Allow-Credentials": true,
      "Access-Control-Allow-Headers": "Content-Type",
      "Access-Control-Allow-Origin": "http://localhost:8080",
    },
    historyApiFallback: {
      disableDotRule: true,
    },
    inline: true,
    port: 8089,
    stats: "minimal",
    target: "http://localhost:8081",
  },
];
