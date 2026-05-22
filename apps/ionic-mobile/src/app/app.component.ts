import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, CUSTOM_ELEMENTS_SCHEMA, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { catchError, of } from 'rxjs';
import { AdmCalcRequest, AdmCalcResultado } from './admcalc.models';
import { AdmCalcService } from './admcalc.service';

type FeatureKey =
  | 'clientes' | 'enderecos' | 'vendas' | 'itens-venda'
  | 'produtos' | 'categorias' | 'nf-entrada' | 'itens-nf'
  | 'despesas' | 'tipos-despesa' | 'formas-pagamento' | 'pagamentos-venda' | 'balanco' | 'admcalc'
  | 'usuarios' | 'perfis' | 'menus';

type View = 'home' | FeatureKey;

interface MobileFeature {
  key: FeatureKey;
  group: string;
  icon: string;
  label: string;
  className: string;
  endpoint: string;
  summary: string;
  operations: string[];
  fields: string[];
}

interface MobileGroup {
  id: string;
  label: string;
  icon: string;
  features: FeatureKey[];
}

@Component({
  selector: 'apex-root',
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class AppComponent {
  readonly collapsed = signal(false);
  readonly theme = signal<'light' | 'dark'>('light');
  readonly activeView = signal<View>('home');
  readonly activeGroup = signal('financas');
  readonly loading = signal(false);
  readonly message = signal('Mobile pronto para operação.');
  readonly resultado = signal<AdmCalcResultado | null>(null);
  readonly syncState = signal<Record<string, string>>({});
  readonly actionState = signal('Toque em uma operação para iniciar o fluxo.');

  readonly groups: MobileGroup[] = [
    { id: 'comercial', label: 'Comercial', icon: 'CO', features: ['clientes', 'enderecos', 'vendas', 'itens-venda'] },
    { id: 'estoqueFiscal', label: 'Estoque/Fiscal', icon: 'EF', features: ['produtos', 'categorias', 'nf-entrada', 'itens-nf'] },
    { id: 'financas', label: 'Finanças', icon: 'FI', features: ['despesas', 'tipos-despesa', 'formas-pagamento', 'pagamentos-venda', 'balanco', 'admcalc'] },
    { id: 'administracao', label: 'Admin', icon: 'AD', features: ['usuarios', 'perfis', 'menus'] }
  ];

  readonly features: MobileFeature[] = [
    { key: 'clientes', group: 'comercial', icon: 'CL', label: 'Clientes CRM', className: 'Cliente.java', endpoint: '/api/clientes', summary: 'Cadastro de cliente, documento, status e endereços vinculados.', operations: ['Listar', 'Criar', 'Editar', 'Desativar', 'Vincular endereço'], fields: ['nomeRazao', 'telefone', 'cpfCnpj', 'status'] },
    { key: 'enderecos', group: 'comercial', icon: 'EN', label: 'Endereços', className: 'Endereco.java', endpoint: '/api/enderecos', summary: 'CEP, cidade, logradouro e dados para entrega/cobrança.', operations: ['Listar', 'Criar', 'Editar', 'Excluir', 'Validar CEP'], fields: ['cep', 'cidade', 'logradouro', 'uf'] },
    { key: 'vendas', group: 'comercial', icon: 'VD', label: 'Vendas', className: 'Venda.java', endpoint: '/api/vendas', summary: 'Venda, cliente, usuário, itens, pagamentos, desconto e cancelamento.', operations: ['Listar', 'Realizar venda', 'Cancelar', 'Conferir desconto'], fields: ['cliente', 'usuario', 'valorTotal', 'itens', 'pagamentos'] },
    { key: 'itens-venda', group: 'comercial', icon: 'IV', label: 'Itens Venda', className: 'ProdutoVenda.java + ProdutoVendaId.java', endpoint: '/api/itensvenda', summary: 'Chave produto/venda, quantidade, custo e preço total.', operations: ['Listar', 'Buscar chave composta', 'Conferir preço', 'Validar quantidade'], fields: ['produtoId', 'vendaId', 'quantidade', 'precoTotal'] },
    { key: 'produtos', group: 'estoqueFiscal', icon: 'PR', label: 'Produtos', className: 'Produto.java', endpoint: '/api/produtos', summary: 'Preço, custo, estoque mínimo, categoria e fornecedor.', operations: ['Listar', 'Criar', 'Editar preço', 'Desativar', 'Monitorar estoque'], fields: ['descricao', 'custo', 'valorVenda', 'quantidadeEstoque'] },
    { key: 'categorias', group: 'estoqueFiscal', icon: 'CT', label: 'Categorias', className: 'Categoria.java', endpoint: '/api/categorias', summary: 'Agrupamento comercial e fiscal do catálogo.', operations: ['Listar', 'Criar', 'Editar', 'Desativar'], fields: ['descricao', 'status'] },
    { key: 'nf-entrada', group: 'estoqueFiscal', icon: 'NF', label: 'NF Entrada', className: 'NotaFiscalEntrada.java', endpoint: '/api/nfs/entrada', summary: 'Entrada fiscal com fornecedor, itens e atualização de estoque.', operations: ['Importar XML', 'Registrar entrada', 'Conferir totais', 'Atualizar estoque'], fields: ['numero', 'serie', 'fornecedor', 'valorTotal'] },
    { key: 'itens-nf', group: 'estoqueFiscal', icon: 'IN', label: 'Itens NF', className: 'ItemNotaFiscal.java', endpoint: '/api/nfs/entrada', summary: 'Produtos, quantidades e custos unitários recebidos por NF.', operations: ['Conferir itens', 'Vincular produto', 'Ajustar quantidade', 'Validar custo'], fields: ['produto', 'quantidade', 'valorCustoUnitario'] },
    { key: 'despesas', group: 'financas', icon: 'DP', label: 'Despesas', className: 'Despesa.java', endpoint: '/api/despesas', summary: 'Contas a pagar, vencimento, pagamento, tipo e cancelamento.', operations: ['Listar', 'Criar despesa', 'Cancelar', 'Conferir vencimentos'], fields: ['descricao', 'valor', 'dataVencimento', 'tipoDespesa'] },
    { key: 'tipos-despesa', group: 'financas', icon: 'TD', label: 'Tipos Despesa', className: 'TipoDespesa.java', endpoint: '/api/despesas/tipos', summary: 'Classificação financeira para DRE e centros de custo.', operations: ['Listar', 'Criar tipo', 'Ativar/desativar', 'Padronizar'], fields: ['nome', 'status'] },
    { key: 'formas-pagamento', group: 'financas', icon: 'FP', label: 'Formas Pagamento', className: 'FormaPagamento.java', endpoint: '/api/formaspagamento', summary: 'Meios de pagamento e regras de liquidação do caixa.', operations: ['Listar', 'Criar forma', 'Editar', 'Desativar'], fields: ['nome', 'descricao', 'tipoPagamento', 'status'] },
    { key: 'pagamentos-venda', group: 'financas', icon: 'PV', label: 'Pagamentos Venda', className: 'VendaPagamento.java + VendaPagamentoId.java', endpoint: '/api/pagamentosvenda', summary: 'Chave venda/forma, valor pago e parcelas para conciliação.', operations: ['Listar', 'Buscar chave composta', 'Conferir parcelas', 'Conciliar'], fields: ['vendaId', 'formaPagamentoId', 'valorPago', 'numeroParcelas'] },
    { key: 'balanco', group: 'financas', icon: 'BL', label: 'Balanço', className: 'RelatorioFinanceiroDTO.java', endpoint: '/api/relatorios/financeiro?inicio=2026-05-01&fim=2026-05-31', summary: 'Receita, CMV, despesas, lucro líquido e saldo de caixa.', operations: ['Gerar balanço', 'Exportar DRE', 'Auditar CMV', 'Projetar caixa'], fields: ['totalVendas', 'totalCustosProdutos', 'totalDespesas', 'lucroLiquido'] },
    { key: 'admcalc', group: 'financas', icon: 'AC', label: 'AdmCalc', className: 'AdmCalcController.java', endpoint: '/api/admcalc/calcular', summary: 'Cálculos trabalhistas, benefícios, férias, 13º e Fator R.', operations: ['Calcular folha', 'Simular Fator R', 'Exportar demonstrativo'], fields: ['salarioMensal', 'jornadaMensal', 'horasExtras', 'receitaBruta'] },
    { key: 'usuarios', group: 'administracao', icon: 'US', label: 'Usuários', className: 'Usuario.java', endpoint: '/api/usuarios', summary: 'Operadores, login, status e vínculo com perfil RBAC.', operations: ['Listar', 'Criar', 'Editar', 'Desativar', 'Buscar login'], fields: ['nome', 'login', 'dataNascimento', 'perfil'] },
    { key: 'perfis', group: 'administracao', icon: 'PF', label: 'Perfis', className: 'Perfil.java', endpoint: '/api/perfis', summary: 'Roles e permissões de acesso aos menus.', operations: ['Listar', 'Criar perfil', 'Editar', 'Vincular menus'], fields: ['nome', 'status', 'menus'] },
    { key: 'menus', group: 'administracao', icon: 'MN', label: 'Menus', className: 'Menu.java', endpoint: '/api/menus', summary: 'Catálogo de menus, links, ícones e perfis autorizados.', operations: ['Listar', 'Criar menu', 'Editar', 'Controlar exibição'], fields: ['nome', 'link', 'icone', 'exibir', 'perfis'] }
  ];

  readonly activeFeature = computed(() => {
    const view = this.activeView();
    return view === 'home' ? undefined : this.featureByKey(view);
  });

  form: AdmCalcRequest = {
    salarioMensal: 3800,
    jornadaMensal: 220,
    percentualHoraExtra: 0.5,
    horasExtras: 9,
    horasNoturnasRelogio: 4,
    adicionalPericulosidade: false,
    salarioMinimo: 1412,
    percentualInsalubridade: 0.1,
    custoPassagens: 220,
    filhosElegiveis: 1,
    cotaSalarioFamilia: 62.04,
    mesesTrabalhados: 10,
    receitaBruta: 52000,
    folhaSalarios: 14600,
    proLabore: 4800
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

  groupFeatures(groupId: string) {
    return this.features.filter((feature) => feature.group === groupId);
  }

  setView(view: View) {
    this.activeView.set(view);
    if (view !== 'home') {
      const feature = this.featureByKey(view);
      this.activeGroup.set(feature?.group ?? this.activeGroup());
      this.message.set(`${feature?.label ?? view} carregado.`);
      this.actionState.set('Toque em uma operação para iniciar o fluxo.');
      return;
    }
    this.message.set('Resumo de paridade carregado.');
  }

  setGroup(groupId: string) {
    this.activeGroup.set(groupId);
    this.message.set(`Grupo ${this.groups.find((group) => group.id === groupId)?.label} selecionado.`);
  }

  toggleCollapsed() {
    this.collapsed.update((value) => !value);
  }

  toggleTheme() {
    this.theme.update((value) => value === 'light' ? 'dark' : 'light');
    document.documentElement.dataset['theme'] = this.theme();
    this.message.set(`Tema ${this.theme() === 'dark' ? 'escuro' : 'claro'} aplicado.`);
  }

  syncFeature(feature: MobileFeature) {
    this.loading.set(true);
    this.http.get<unknown>(feature.endpoint).pipe(
      catchError(() => {
        this.syncState.update((state) => ({ ...state, [feature.key]: 'Offline local' }));
        this.message.set(`${feature.label}: API indisponível, fluxo local mantido.`);
        return of(null);
      })
    ).subscribe((payload) => {
      this.loading.set(false);
      if (payload !== null) {
        const count = Array.isArray(payload) ? payload.length : 1;
        this.syncState.update((state) => ({ ...state, [feature.key]: `${count} registro(s)` }));
        this.message.set(`${feature.label}: sincronizado com sucesso.`);
      }
    });
  }

  runOperation(feature: MobileFeature, operation: string) {
    this.actionState.set(`${feature.label}: ${operation} preparado. Valide permissões, dados obrigatórios e auditoria antes de salvar.`);
    this.message.set(`${operation} iniciado em ${feature.label}.`);
  }

  calcular() {
    this.loading.set(true);
    this.admCalcService.calcular(this.form).subscribe((resultado) => {
      this.resultado.set(resultado);
      this.loading.set(false);
      this.message.set('AdmCalc calculado e pronto para conferência.');
    });
  }
}
