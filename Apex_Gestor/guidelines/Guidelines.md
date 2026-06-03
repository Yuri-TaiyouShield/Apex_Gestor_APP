# Apex Gestor Frontend Guidelines

Este arquivo consolida diretrizes de produto e interface para manter o Apex Gestor consistente entre Web, Mobile e Desktop.

## Experiencia

- Priorizar produtividade, leitura rapida e baixo atrito operacional.
- Usar linguagem clara em mensagens de erro, sempre indicando a acao que o usuario pode tentar.
- Manter modo claro e escuro com contraste adequado para uso em loja, estoque, caixa e escritorio.
- Preservar navegacao lateral hierarquica para areas como Vendas, Estoque, Financas, Fiscal, Pessoas e Configuracoes.

## Interface

- Componentes reutilizaveis devem seguir os tokens de cor, espacamento e tipografia do projeto.
- Botoes precisam ter estado normal, hover, focus, active, loading e disabled.
- Formularios devem validar dados antes de enviar para a API e exibir feedback junto ao campo afetado.
- Tabelas e listas devem ser densas o suficiente para uso administrativo, sem perder legibilidade.

## Acessibilidade

- Todo controle interativo deve ser acessivel por teclado.
- Textos e fundos devem respeitar contraste AA ou superior.
- Icones acionaveis devem possuir rotulo acessivel.
- Estados criticos como erro, bloqueio e sucesso devem combinar cor, texto e semantica.

## Performance

- Preferir carregamento preguiçoso para telas pesadas.
- Evitar dependencias duplicadas ou assets sem uso.
- Manter builds de producao minificados, sem source maps publicos e com cache de assets versionados.
