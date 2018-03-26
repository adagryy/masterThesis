namespace serwer.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class Firstname_lastname_pesel : DbMigration
    {
        public override void Up()
        {
            AddColumn("dbo.AspNetUsers", "Firstname", c => c.String());
            AddColumn("dbo.AspNetUsers", "Lastname", c => c.String());
            AddColumn("dbo.AspNetUsers", "pesel_number", c => c.String());
        }
        
        public override void Down()
        {
            DropColumn("dbo.AspNetUsers", "pesel_number");
            DropColumn("dbo.AspNetUsers", "Lastname");
            DropColumn("dbo.AspNetUsers", "Firstname");
        }
    }
}
