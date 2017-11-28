using Microsoft.Owin;
using Owin;
using System;

[assembly: OwinStartupAttribute(typeof(serwer.Startup))]
namespace serwer
{
    public partial class Startup
    {
        public void Configuration(IAppBuilder app)
        {
            ConfigureAuth(app);
        }
    }
}
