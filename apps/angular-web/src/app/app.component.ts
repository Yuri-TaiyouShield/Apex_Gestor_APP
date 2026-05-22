import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, of } from 'rxjs';
import { AdmCalcRequest, AdmCalcResultado } from './admcalc.models';
import { AdmCalcService } from './admcalc.service';

type FeatureKey =
  | 'clientes'
  | 'enderecos'
  | 'vendas'
  | 'itens-venda'
  | 'produtos'
  | 'categorias'
  | 'nf-entrada'
  | 'itens-nf'
  | 'despesas'
  | 'tipos-despesa'
  | 'formas-pagamento'
  | 'pagamentos-venda'
  | 'balanco'
  | 'admcalc'
  | 'usuarios'
  | 'perfis'
  | 'menus';

type View = 'dashboard' | FeatureKey;

interface DomainFeature {
  key: FeatureKey;
  group: string;
  icon: string;
  label: string;
  className: string;
  endpoint: string;
  description: string;
  operations: string[];
  fields: string[];
  sampleRows: Array<Record<string, string>>;
}

interface NavGroup {
  id: string;
  label: string;
  icon: string;
  summary: string;
  features: FeatureKey[];
}

@Component({
  selector: 'apex-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  readonly sidebarCollapsed = signal(false);
  readonly theme = signal<'light' | 'dark'>('light');
  readonly activeView = signal<View>('dashboard');
  readonly expandedGroups = signal<Record<string, boolean>>({
    comercial: true,
    estoqueFiscal: true,
    financas: true,
    administracao: true
  });
  readonly toast = signal('Interface Angular pronta para operação.');
  readonly loading = signal(false);
  readonly resultado = signal<AdmCalcResultado | null>(null);
  readonly featureData = signal<Record<string, unknown>>({});
  readonly actionLog = signal('Selecione uma operação para registrar o fluxo de trabalho.');

  readonly features: DomainFeature[] = [
    {
      key: 'clientes',
      group: 'comercial',
      icon: 'CL',
      label: 'Clientes CRM',
      className: 'Cliente.java',
      endpoint: '/api/clientes',
      description: 'Cadastro, atualização, consulta e desativação de clientes B2B/B2C.',
      operations: ['Listar clientes', 'Criar cliente', 'Editar dados cadastrais', 'Desativar cliente', 'Vincular endereços'],
      fields: ['idCliente', 'nomeRazao', 'telefone', 'tipoDocumento', 'cpfCnpj', 'dataCadastro', 'status', 'enderecos'],
      sampleRows: [
        { Registro: 'Cliente padrão', Documento: 'CPF/CNPJ validado', Status: 'Ativo' },
        { Registro: 'Cliente obra', Documento: 'CNPJ', Status: 'Com limite' }
      ]
    },
    {
      key: 'enderecos',
      group: 'comercial',
      icon: 'EN',
      label: 'Endereços',
      className: 'Endereco.java',
      endpoint: '/api/enderecos',
      description: 'Base de entrega, cobrança e cadastro territorial de clientes e fornecedores.',
      operations: ['Listar endereços', 'Criar endereço', 'Editar endereço', 'Excluir endereço', 'Validar CEP'],
      fields: ['idEndereco', 'estado', 'cidade', 'cep', 'bairro', 'logradouro', 'uf', 'numero', 'complemento'],
      sampleRows: [
        { Cidade: 'São Paulo', UF: 'SP', Uso: 'Entrega' },
        { Cidade: 'Campinas', UF: 'SP', Uso: 'Cobrança' }
      ]
    },
    {
      key: 'vendas',
      group: 'comercial',
      icon: 'VD',
      label: 'Vendas',
      className: 'Venda.java',
      endpoint: '/api/vendas',
      description: 'Registro de venda, baixa de estoque, descontos, cancelamento e observações comerciais.',
      operations: ['Listar vendas', 'Realizar venda', 'Cancelar venda', 'Auditar desconto', 'Conferir cliente e caixa'],
      fields: ['idVenda', 'usuario', 'cliente', 'status', 'valorTotal', 'desconto', 'observacao', 'dataVenda', 'itens', 'pagamentos'],
      sampleRows: [
        { Venda: '#1042', Total: 'R$ 1.840,00', Status: 'Finalizada' },
        { Venda: '#1043', Total: 'R$ 620,00', Status: 'Em separação' }
      ]
    },
    {
      key: 'itens-venda',
      group: 'comercial',
      icon: 'IV',
      label: 'Itens de Venda',
      className: 'ProdutoVenda.java + ProdutoVendaId.java',
      endpoint: '/api/itensvenda',
      description: 'Composição dos produtos vendidos com chave composta produto/venda.',
      operations: ['Listar itens', 'Buscar por produtoId e vendaId', 'Conferir quantidade', 'Validar preço total'],
      fields: ['produtoId', 'vendaId', 'produto', 'venda', 'quantidade', 'precoUnitario', 'custoUnitario', 'precoTotal'],
      sampleRows: [
        { Produto: 'Cimento CP II', Quantidade: '12', Total: 'R$ 468,00' },
        { Produto: 'Tinta acrílica', Quantidade: '2', Total: 'R$ 396,00' }
      ]
    },
    {
      key: 'produtos',
      group: 'estoqueFiscal',
      icon: 'PR',
      label: 'Produtos',
      className: 'Produto.java',
      endpoint: '/api/produtos',
      description: 'Cadastro de itens, preço, custo, categoria, fornecedor e controle de estoque mínimo.',
      operations: ['Listar produtos', 'Criar produto', 'Editar preço/custo', 'Desativar produto', 'Monitorar estoque mínimo'],
      fields: ['idProduto', 'descricao', 'custo', 'codigoBarras', 'marca', 'unidadeMedida', 'quantidadeEstoque', 'estoqueMinimo', 'valorVenda', 'categoria', 'fornecedor'],
      sampleRows: [
        { Produto: 'Cimento CP II 50kg', Saldo: '22', Status: 'Baixo' },
        { Produto: 'Disjuntor bipolar 32A', Saldo: '7', Status: 'Crítico' }
      ]
    },
    {
      key: 'categorias',
      group: 'estoqueFiscal',
      icon: 'CT',
      label: 'Categorias',
      className: 'Categoria.java',
      endpoint: '/api/categorias',
      description: 'Classificação comercial e fiscal para filtros, precificação e relatórios de estoque.',
      operations: ['Listar categorias', 'Criar categoria', 'Editar categoria', 'Desativar categoria'],
      fields: ['idCategoria', 'descricao', 'status'],
      sampleRows: [
        { Categoria: 'Obra pesada', Status: 'Ativa' },
        { Categoria: 'Acabamento', Status: 'Ativa' }
      ]
    },
    {
      key: 'nf-entrada',
      group: 'estoqueFiscal',
      icon: 'NF',
      label: 'NF de Entrada',
      className: 'NotaFiscalEntrada.java',
      endpoint: '/api/nfs/entrada',
      description: 'Entrada fiscal de mercadorias com fornecedor, totais e impacto direto no estoque.',
      operations: ['Importar XML', 'Registrar entrada', 'Conferir totais', 'Atualizar custo médio', 'Gerar auditoria fiscal'],
      fields: ['idNfEntrada', 'numero', 'serie', 'fornecedor', 'dataEmissao', 'dataEntrada', 'valorTotal', 'itens'],
      sampleRows: [
        { Nota: '000128', Fornecedor: 'Distribuidora Apex', Total: 'R$ 18.400,00' },
        { Nota: '000129', Fornecedor: 'Atacado Obra', Total: 'R$ 7.950,00' }
      ]
    },
    {
      key: 'itens-nf',
      group: 'estoqueFiscal',
      icon: 'IN',
      label: 'Itens da NF',
      className: 'ItemNotaFiscal.java',
      endpoint: '/api/nfs/entrada',
      description: 'Itens importados da nota fiscal para atualização de estoque e custo unitário.',
      operations: ['Conferir itens importados', 'Validar produto vinculado', 'Ajustar quantidade', 'Conferir custo unitário'],
      fields: ['idItemNf', 'notaFiscal', 'produto', 'quantidade', 'valorCustoUnitario'],
      sampleRows: [
        { Produto: 'Cabo flexível', Quantidade: '180', Custo: 'R$ 3,40' },
        { Produto: 'Argamassa AC-II', Quantidade: '90', Custo: 'R$ 18,90' }
      ]
    },
    {
      key: 'despesas',
      group: 'financas',
      icon: 'DP',
      label: 'Gestão de Despesas',
      className: 'Despesa.java',
      endpoint: '/api/despesas',
      description: 'Contas a pagar, vencimentos, pagamentos, cancelamentos e impacto no fluxo de caixa.',
      operations: ['Listar despesas', 'Criar despesa', 'Cancelar despesa', 'Conferir vencimentos', 'Classificar por tipo'],
      fields: ['idDespesa', 'descricao', 'valor', 'dataVencimento', 'dataPagamento', 'tipoDespesa', 'status'],
      sampleRows: [
        { Despesa: 'Energia loja matriz', Valor: 'R$ 1.420,00', Status: 'A vencer' },
        { Despesa: 'Frete fornecedor', Valor: 'R$ 690,00', Status: 'Pago' }
      ]
    },
    {
      key: 'tipos-despesa',
      group: 'financas',
      icon: 'TD',
      label: 'Tipos de Despesa',
      className: 'TipoDespesa.java',
      endpoint: '/api/despesas/tipos',
      description: 'Taxonomia financeira para relatórios, DRE, centros de custo e projeções.',
      operations: ['Listar tipos', 'Criar tipo', 'Ativar/desativar tipo', 'Padronizar centro de custo'],
      fields: ['idTipoDespesa', 'nome', 'status'],
      sampleRows: [
        { Tipo: 'Operacional', Status: 'Ativo', Uso: 'DRE' },
        { Tipo: 'Logística', Status: 'Ativo', Uso: 'CMV expandido' }
      ]
    },
    {
      key: 'formas-pagamento',
      group: 'financas',
      icon: 'FP',
      label: 'Formas de Pagamento',
      className: 'FormaPagamento.java',
      endpoint: '/api/formaspagamento',
      description: 'Meios de pagamento, tipo, status e regras de liquidação no caixa.',
      operations: ['Listar formas', 'Criar forma', 'Editar taxa/prazo', 'Desativar forma', 'Vincular ao PDV'],
      fields: ['idFormaPagamento', 'nome', 'descricao', 'status', 'tipoPagamento'],
      sampleRows: [
        { Forma: 'PIX', Tipo: 'À vista', Status: 'Ativa' },
        { Forma: 'Cartão crédito', Tipo: 'Parcelado', Status: 'Ativa' }
      ]
    },
    {
      key: 'pagamentos-venda',
      group: 'financas',
      icon: 'PV',
      label: 'Pagamentos de Venda',
      className: 'VendaPagamento.java + VendaPagamentoId.java',
      endpoint: '/api/pagamentosvenda',
      description: 'Liquidação da venda com chave composta venda/forma de pagamento.',
      operations: ['Listar pagamentos', 'Buscar por vendaId e formaPagamentoId', 'Conferir parcelas', 'Conciliar valor pago'],
      fields: ['vendaId', 'formaPagamentoId', 'venda', 'formaPagamento', 'valorPago', 'numeroParcelas'],
      sampleRows: [
        { Venda: '#1042', Forma: 'PIX', Valor: 'R$ 840,00' },
        { Venda: '#1042', Forma: 'Crédito', Valor: 'R$ 1.000,00' }
      ]
    },
    {
      key: 'balanco',
      group: 'financas',
      icon: 'BL',
      label: 'Balanço e Relatórios',
      className: 'RelatorioFinanceiroDTO.java',
      endpoint: '/api/relatorios/financeiro?inicio=2026-05-01&fim=2026-05-31',
      description: 'Agregação financeira para receita, custos, despesas, lucro líquido e saldo de caixa.',
      operations: ['Gerar balanço', 'Comparar período', 'Exportar DRE', 'Auditar CMV', 'Projetar saldo de caixa'],
      fields: ['totalVendas', 'totalCustosProdutos', 'totalDespesas', 'lucroLiquido', 'saldoCaixa'],
      sampleRows: [
        { Indicador: 'Receita', Valor: 'R$ 82.000,00', Tendência: '+12%' },
        { Indicador: 'Lucro líquido', Valor: 'R$ 18.450,00', Tendência: 'Saudável' }
      ]
    },
    {
      key: 'admcalc',
      group: 'financas',
      icon: 'AC',
      label: 'AdmCalc Financeiro',
      className: 'AdmCalcController.java',
      endpoint: '/api/admcalc/calcular',
      description: 'Cálculos trabalhistas, adicionais, benefícios, férias, 13º e Fator R.',
      operations: ['Calcular folha', 'Simular Fator R', 'Exportar demonstrativo', 'Conferir adicionais'],
      fields: ['salarioMensal', 'jornadaMensal', 'horasExtras', 'adicionais', 'receitaBruta', 'folhaSalarios'],
      sampleRows: [
        { Simulação: 'Folha mensal', Resultado: 'Fator R saudável', Status: 'Pronto' }
      ]
    },
    {
      key: 'usuarios',
      group: 'administracao',
      icon: 'US',
      label: 'Usuários',
      className: 'Usuario.java',
      endpoint: '/api/usuarios',
      description: 'Cadastro de operadores, login, status e vínculo com perfil de acesso.',
      operations: ['Listar usuários', 'Criar usuário', 'Editar usuário', 'Desativar usuário', 'Buscar por login'],
      fields: ['idUsuario', 'nome', 'login', 'senha', 'dataNascimento', 'status', 'perfil'],
      sampleRows: [
        { Usuário: 'Caixa Loja 01', Perfil: 'ROLE_CAIXA', Status: 'Ativo' },
        { Usuário: 'Financeiro', Perfil: 'ROLE_FINANCEIRO', Status: 'Ativo' }
      ]
    },
    {
      key: 'perfis',
      group: 'administracao',
      icon: 'PF',
      label: 'Perfis RBAC',
      className: 'Perfil.java',
      endpoint: '/api/perfis',
      description: 'Papéis de acesso e governança de permissões por menus do sistema.',
      operations: ['Listar perfis', 'Criar perfil', 'Editar perfil', 'Desativar perfil', 'Vincular menus'],
      fields: ['idPerfil', 'nome', 'status', 'menus'],
      sampleRows: [
        { Perfil: 'ROLE_GESTOR', Acesso: 'Completo', Status: 'Ativo' },
        { Perfil: 'ROLE_VENDEDOR', Acesso: 'Comercial', Status: 'Ativo' }
      ]
    },
    {
      key: 'menus',
      group: 'administracao',
      icon: 'MN',
      label: 'Menus do Sistema',
      className: 'Menu.java',
      endpoint: '/api/menus',
      description: 'Catálogo de menus, links, ícones, exibição e vínculos por perfil.',
      operations: ['Listar menus', 'Criar menu', 'Editar menu', 'Desativar menu', 'Controlar exibição por perfil'],
      fields: ['idMenu', 'nome', 'link', 'icone', 'exibir', 'perfis'],
      sampleRows: [
        { Menu: 'Finanças', Link: '/financas', Exibir: 'Sim' },
        { Menu: 'Estoque', Link: '/estoque', Exibir: 'Sim' }
      ]
    }
  ];

  readonly navGroups: NavGroup[] = [
    {
      id: 'comercial',
      label: 'Comercial',
      icon: 'CO',
      summary: 'CRM, vendas, itens e endereços.',
      features: ['clientes', 'enderecos', 'vendas', 'itens-venda']
    },
    {
      id: 'estoqueFiscal',
      label: 'Estoque e Fiscal',
      icon: 'EF',
      summary: 'Produtos, categorias, notas e itens fiscais.',
      features: ['produtos', 'categorias', 'nf-entrada', 'itens-nf']
    },
    {
      id: 'financas',
      label: 'Finanças',
      icon: 'FI',
      summary: 'Despesas, pagamentos, balanço e AdmCalc.',
      features: ['despesas', 'tipos-despesa', 'formas-pagamento', 'pagamentos-venda', 'balanco', 'admcalc']
    },
    {
      id: 'administracao',
      label: 'Administração',
      icon: 'AD',
      summary: 'Usuários, perfis RBAC e menus.',
      features: ['usuarios', 'perfis', 'menus']
    }
  ];

  readonly activeFeature = computed(() => {
    const current = this.activeView();
    return current === 'dashboard' ? undefined : this.featureByKey(current);
  });

  readonly title = computed(() => {
    const feature = this.activeFeature();
    return feature ? feature.label : 'Painel de paridade funcional';
  });

  form: AdmCalcRequest = {
    salarioMensal: 4200,
    jornadaMensal: 220,
    percentualHoraExtra: 0.5,
    horasExtras: 12,
    horasNoturnasRelogio: 8,
    adicionalPericulosidade: true,
    salarioMinimo: 1412,
    percentualInsalubridade: 0.2,
    custoPassagens: 310,
    filhosElegiveis: 1,
    cotaSalarioFamilia: 62.04,
    mesesTrabalhados: 12,
    receitaBruta: 82000,
    folhaSalarios: 24600,
    proLabore: 6500
  };

  constructor(
    private readonly admCalcService: AdmCalcService,
    private readonly http: HttpClient
  ) {
    document.documentElement.dataset['theme'] = this.theme();
  }

  featureByKey(key: FeatureKey) {
    return this.features.find((feature) => feature.key === key);
  }

  setView(view: View) {
    this.activeView.set(view);
    this.actionLog.set('Selecione uma operação para registrar o fluxo de trabalho.');
    this.toast.set(`Tela ${this.title()} carregada.`);
  }

  toggleGroup(groupId: string) {
    this.expandedGroups.update((groups) => ({ ...groups, [groupId]: !groups[groupId] }));
  }

  toggleSidebar() {
    this.sidebarCollapsed.update((value) => !value);
  }

  toggleTheme() {
    this.theme.update((value) => value === 'light' ? 'dark' : 'light');
    document.documentElement.dataset['theme'] = this.theme();
    this.toast.set(`Tema ${this.theme() === 'dark' ? 'escuro' : 'claro'} aplicado.`);
  }

  syncFeature(feature: DomainFeature) {
    this.loading.set(true);
    this.http.get<unknown>(feature.endpoint).pipe(
      catchError((error) => {
        this.toast.set(`API indisponível para ${feature.label}. Mantive a tela em modo operacional local.`);
        this.featureData.update((data) => ({ ...data, [feature.key]: { offline: true, message: error?.message ?? 'Falha de conexão' } }));
        return of(null);
      })
    ).subscribe((payload) => {
      this.loading.set(false);
      if (payload !== null) {
        const count = Array.isArray(payload) ? payload.length : 1;
        this.featureData.update((data) => ({ ...data, [feature.key]: payload }));
        this.toast.set(`${feature.label}: sincronização concluída com ${count} registro(s).`);
      }
    });
  }

  featureSyncLabel(feature: DomainFeature) {
    const payload = this.featureData()[feature.key];
    if (!payload) {
      return 'Ainda não sincronizado';
    }
    if ((payload as { offline?: boolean }).offline) {
      return 'API indisponível, usando operação local';
    }
    return Array.isArray(payload) ? `${payload.length} registro(s) recebidos` : 'Resumo recebido da API';
  }

  runFeatureAction(feature: DomainFeature, operation: string) {
    this.actionLog.set(`${feature.label}: operação "${operation}" registrada. Próximo passo: validar dados, permissões e auditoria antes de salvar.`);
    this.toast.set(`${operation} preparado em ${feature.label}.`);
  }

  calcularAdmCalc() {
    this.loading.set(true);
    this.admCalcService.calcular(this.form).subscribe((resultado) => {
      this.resultado.set(resultado);
      this.loading.set(false);
      this.toast.set('AdmCalc calculado com sucesso. Resultados prontos para conferência.');
    });
  }

  sync() {
    const feature = this.activeFeature();
    if (feature && feature.key !== 'admcalc') {
      this.syncFeature(feature);
      return;
    }
    this.toast.set('Sincronização solicitada. A API será usada quando estiver disponível.');
  }

  exportarResumo() {
    this.toast.set('Resumo financeiro preparado para exportação em PDF na próxima etapa.');
  }
}
