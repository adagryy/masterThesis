using Microsoft.Owin;
using Owin;
using serwer.Config;
using System;

[assembly: OwinStartupAttribute(typeof(serwer.Startup))]
namespace serwer
{
    public partial class Startup
    {
        public void Configuration(IAppBuilder app)
        {
            ConfigureAuth(app);
            ServerConfigurator.initializeStorageDirectories();
        }
    }
}
