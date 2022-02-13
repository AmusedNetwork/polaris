mod create;
mod delete;
mod view;

use crate::api_dispatch;
use crate::cmd::token::create::TokenCreate;
use crate::cmd::token::delete::TokenDelete;
use crate::cmd::token::view::TokenView;
use crate::cmd::Command;
use clap::{Parser, Subcommand};

#[derive(Debug, Parser)]
pub struct Token {
    pub profile: Option<String>,

    #[clap(long)]
    pub json: bool,

    #[clap(subcommand)]
    pub subcommand: TokenSubcommand,
}

#[derive(Debug, Subcommand)]
pub enum TokenSubcommand {
    Create(TokenCreate),
    Delete(TokenDelete),
    View(TokenView),
}

#[async_trait::async_trait(?Send)]
impl Command for Token {
    async fn run(&self) -> anyhow::Result<()> {
        api_dispatch!(TokenSubcommand, self, Create, Delete, View);
        Ok(())
    }
}