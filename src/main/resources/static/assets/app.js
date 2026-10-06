'use strict';
const $ = selector => document.querySelector(selector);
const state = {user:null, authorization:null, cache:{}, page:'dashboard', search:'', roleFilter:'Todos', tablePage:1, generation:0};
const labels = {ADMIN:'Administrador',DIRECTOR:'Director',COORDENADOR:'Coordenador',PROFESSOR:'Professor',SECRETARIA:'Secretaria',ALUNO:'Aluno',ENCARREGADO:'Encarregado'};
const management = ['ADMIN','DIRECTOR','COORDENADOR','SECRETARIA'];
const writers = ['ADMIN','SECRETARIA'];
const privileged = ['ADMIN','DIRECTOR','COORDENADOR','SECRETARIA'];
const icons = {dashboard:'⌂',utilizadores:'♙',turmas:'▤',alunos:'♧',professores:'♙',encarregados:'♧',integracoes:'◇',comunicacao:'✉',reunioes:'▦',relatorios:'▥'};
const escapeHTML = value => String(value ?? '').replace(/[&<>"']/g, character => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[character]));
const initials = name => String(name || '?').trim().split(/\s+/).slice(0,2).map(part => part[0]).join('').toUpperCase();
const canWrite = () => writers.includes(state.user?.role);
const canManageUser = user => state.user?.role==='ADMIN' || state.user?.role==='SECRETARIA' && !privileged.includes(user.role);
const pageTitle = {dashboard:'Painel de gestão',utilizadores:'Utilizadores',turmas:'Turmas',alunos:'Alunos',professores:'Professores',encarregados:'Encarregados',integracoes:'Central de Integrações',comunicacao:'Canais de Comunicação',reunioes:'Reuniões',relatorios:'Relatórios',perfil:'Meu perfil'};
const readable = type => {
  if(type==='utilizadores') return writers.includes(state.user?.role);
  if(type==='alunos'||type==='encarregados') return management.includes(state.user?.role);
  return [...management,'PROFESSOR'].includes(state.user?.role);
};
const endpoint = type => type==='utilizadores'?'/api/auth/utilizadores':'/api/'+type;
const dateText = date => date ? new Intl.DateTimeFormat('pt-MZ').format(new Date(date+'T12:00:00')) : 'Não indicada';
function toast(message, failure=false) {
  const node=$('#toast'); node.textContent=message;node.classList.toggle('failure',failure);node.hidden=false;
  clearTimeout(toast.timer);toast.timer=setTimeout(()=>node.hidden=true,6000);
}
function logout(message) {
  state.generation++;state.user=null;state.authorization=null;state.cache={};
  $('#modal').close();$('#modal-body').replaceChildren();$('#app-screen').hidden=true;$('#login-screen').hidden=false;
  $('#login-form').reset();$('#login-password').type='password';$('#toggle-password').textContent='Mostrar';
  $('#login-error').hidden=!message;if(message)$('#login-error').textContent=message;
  $('#navigation').replaceChildren();$('#content').replaceChildren();$('#login-username').focus();
}
async function api(path, {method='GET', body, authorization=state.authorization}={}) {
  const headers={'Accept':'application/json','X-Requested-With':'XMLHttpRequest'};
  if(authorization)headers.Authorization=authorization;
  if(body!==undefined)headers['Content-Type']='application/json';
  let response;
  try {response=await fetch(path,{method,headers,body:body===undefined?undefined:JSON.stringify(body),cache:'no-store',credentials:'omit'});}
  catch(error) {throw new Error('Não foi possível contactar o servidor. Verifique se a aplicação está em execução.');}
  const raw=await response.text();let data;try {data=raw?JSON.parse(raw):null;}catch{data=null;}
  if(!response.ok) {
    if(response.status===401) {
      if(state.user)logout('O acesso expirou ou a conta foi alterada. Entre novamente.');
      throw new Error('Nome de utilizador ou palavra-passe inválidos, ou conta desactivada.');
    }
    const detail=data?.detalhes?.join(' · ');
    throw new Error(detail || data?.error || (response.status===403?'Não tem permissão para esta operação.':response.status===409?'O registo já existe ou está associado a outros dados.':'Não foi possível concluir a operação.'));
  }
  return data;
}
async function load(type) {
  const generation=state.generation;
  const result=await api(endpoint(type));
  if(generation===state.generation)state.cache[type]=result;
  return result;
}
function heading(title,description,action='') {
  return '<div class="page-heading"><div><span class="eyebrow">COLÉGIO SAPIENS</span><h1>'+escapeHTML(title)+'</h1><p>'+escapeHTML(description)+'</p></div>'+action+'</div>';
}
function nav() {
  const entries=[['dashboard','Dashboard'],['utilizadores','Utilizadores'],['turmas','Turmas'],['alunos','Alunos'],['professores','Professores'],['encarregados','Encarregados'],['integracoes','Integrações'],['comunicacao','Comunicação'],['reunioes','Reuniões'],['relatorios','Relatórios']];
  $('#navigation').innerHTML=entries.filter(([id])=>!(id in schemas)||readable(id)).map(([id,label])=>'<a href="#'+id+'" class="'+(state.page===id?'active':'')+'" '+(state.page===id?'aria-current="page"':'')+'><span aria-hidden="true">'+icons[id]+'</span>'+label+(['integracoes','comunicacao','reunioes'].includes(id)?'<span class="nav-dot" title="Em preparação"></span>':'')+'</a>').join('');
}
async function renderPage() {
  if(!state.user)return;
  const requested=location.hash.slice(1)||'dashboard';
  state.page=pageTitle[requested]?requested:'dashboard';state.search='';state.roleFilter='Todos';state.tablePage=1;
  const page=state.page,generation=state.generation;
  nav();$('.sidebar').classList.remove('open');$('#breadcrumb').textContent=pageTitle[page];
  $('#content').innerHTML='<div class="loading" role="status">A carregar…</div>';
  try {
    if(page in schemas) {
      if(!readable(page))throw new Error('O seu perfil não tem acesso a esta área.');
      await load(page);if(state.page!==page||generation!==state.generation)return;renderList();
    } else if(page==='dashboard'||page==='relatorios') {
      const types=Object.keys(schemas).filter(readable);
      const results=await Promise.allSettled(types.map(load));
      if(state.page!==page||generation!==state.generation)return;
      const failure=results.find(result=>result.status==='rejected');
      if(failure)throw failure.reason;
      page==='dashboard'?renderDashboard():renderReports();
    } else if(page==='perfil')renderProfile();else renderModule(page);
  }catch(error){
    if(state.user&&state.page===page&&generation===state.generation) {
      $('#content').innerHTML=heading(pageTitle[page],'')+'<div class="panel empty"><h3>Não foi possível carregar</h3><p>'+escapeHTML(error.message)+'</p><button id="retry" class="secondary">Tentar novamente</button></div>';
      $('#retry').onclick=renderPage;
    }
  }
}
function renderDashboard() {
  const role=state.user.role, academic=management.includes(role)||role==='PROFESSOR';
  const types=(writers.includes(role)?['utilizadores']:[]).concat(['turmas','alunos','professores','encarregados'].filter(readable)).slice(0,4);
  $('#content').innerHTML=heading('Olá, '+state.user.nome+'!','Aqui está o estado dos registos do colégio hoje.')+
  (academic?'<div class="cards">'+types.map(type=>'<a class="stat-card" href="#'+type+'"><div class="stat-top">'+pageTitle[type]+'<span class="stat-icon" aria-hidden="true">'+icons[type]+'</span></div><div class="stat-value">'+state.cache[type].length+'</div><div class="stat-caption">Registos na base do colégio →</div></a>').join('')+'</div>':'<div class="notice"><b>Bem-vindo ao seu espaço.</b> O acesso individual a dados de alunos e encarregados ficará disponível numa próxima etapa.</div>')+
  '<div class="dashboard-grid"><section class="panel"><div class="panel-header"><h2>Acesso rápido</h2><span class="badge blue">O seu espaço</span></div><div class="panel-body quick-links">'+
  (academic?['turmas','alunos','professores','encarregados'].filter(readable).map(type=>'<a class="quick-link" href="#'+type+'"><b>'+pageTitle[type]+' →</b><span>Consultar '+(canWrite()?'e gerir ':'')+'os registos</span></a>').join(''):'<a class="quick-link" href="#perfil"><b>Meu perfil →</b><span>Consultar os dados da sua conta</span></a>')+
  '</div></section><section class="panel"><div class="panel-header"><h2>Estado das funcionalidades</h2></div><div class="panel-body"><div class="status-row"><span>Cadastros académicos</span><span class="badge">Disponível</span></div><div class="status-row"><span>Integração externa</span><span class="badge inactive">Em preparação</span></div><div class="status-row"><span>Comunicação e reuniões</span><span class="badge inactive">Em preparação</span></div></div></section></div>';
}
const schemas = {
  utilizadores:{singular:'utilizador',description:'Gerencie quem pode aceder à plataforma.',columns:['Utilizador','Perfil','Estado','Contacto','Acções']},
  turmas:{singular:'turma',description:'Organize as classes, os turnos e os anos lectivos.',columns:['Código','Classe','Ano lectivo','Turno','Acções']},
  alunos:{singular:'aluno',description:'Consulte os alunos e as suas ligações a turmas e encarregados.',columns:['Aluno','Matrícula','Turma','Encarregados','Acções']},
  professores:{singular:'professor',description:'Gerencie os dados dos professores do colégio.',columns:['Professor','N.º funcionário','E-mail','Telefone','Acções']},
  encarregados:{singular:'encarregado',description:'Mantenha os contactos dos encarregados de educação actualizados.',columns:['Encarregado','Documento','Telefone','E-mail','Acções']}
};
function rowActions(type,row) {
  let html='<button data-action="view" data-id="'+row.id+'">Ver</button>';
  if(canWrite() && (type!=='utilizadores'||canManageUser(row)))html+='<button data-action="edit" data-id="'+row.id+'">Editar</button>';
  if(state.user.role==='ADMIN'&&type!=='utilizadores')html+='<button class="danger" data-action="delete" data-id="'+row.id+'">Apagar</button>';
  if(state.user.role==='ADMIN'&&type==='utilizadores'&&row.id!==state.user.id)html+='<button '+(row.activo?'class="danger"':'')+' data-action="state" data-id="'+row.id+'">'+(row.activo?'Desactivar':'Activar')+'</button>';
  return '<div class="row-actions">'+html+'</div>';
}
const person = (name,subtitle) => '<div class="row-name"><span class="avatar">'+escapeHTML(initials(name))+'</span><div><strong>'+escapeHTML(name)+'</strong>'+(subtitle?'<small>'+escapeHTML(subtitle)+'</small>':'')+'</div></div>';
function rowHTML(type,row) {
  const values={
    utilizadores:()=>[person(row.nome,row.username),escapeHTML(labels[row.role]),'<span class="badge '+(row.activo?'':'inactive')+'">'+(row.activo?'Activo':'Inactivo')+'</span>',escapeHTML(row.email||row.telefone||'—')],
    turmas:()=>['<strong>'+escapeHTML(row.codigo)+'</strong>',escapeHTML(row.classe),row.anoLetivo,escapeHTML(row.turno||'—')],
    alunos:()=>[person(row.nome,row.dataNascimento?dateText(row.dataNascimento):''),escapeHTML(row.numeroMatricula),escapeHTML(row.turma?.codigo||'—'),escapeHTML((row.encarregados||[]).map(item=>item.nome).join(', ')||'Nenhum associado')],
    professores:()=>[person(row.nome),escapeHTML(row.numeroFuncionario),escapeHTML(row.email||'—'),escapeHTML(row.telefone||'—')],
    encarregados:()=>[person(row.nome),escapeHTML(row.documento),escapeHTML(row.telefone),escapeHTML(row.email||'—')]
  };
  return '<tr>'+[...values[type](),rowActions(type,row)].map(cell=>'<td>'+cell+'</td>').join('')+'</tr>';
}
function filteredRows() {
  return (state.cache[state.page]||[]).filter(row=> {
    const haystack=JSON.stringify(row).normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
    const term=state.search.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
    return haystack.includes(term)&&(state.roleFilter==='Todos'||row.role===state.roleFilter);
  });
}
function updateTable() {
  const rows=filteredRows(),pages=Math.max(1,Math.ceil(rows.length/10));state.tablePage=Math.min(state.tablePage,pages);
  const shown=rows.slice((state.tablePage-1)*10,state.tablePage*10);
  $('#table-body').innerHTML=shown.length?shown.map(row=>rowHTML(state.page,row)).join(''):'<tr><td colspan="5" class="empty"><h3>'+(state.search||state.roleFilter!=='Todos'?'Nenhum resultado encontrado':'Ainda não há registos')+'</h3><p>'+(canWrite()?'Use o botão de criação para adicionar o primeiro registo.':'Os registos disponíveis aparecerão aqui.')+'</p></td></tr>';
  $('#row-count').textContent=rows.length+' registo'+(rows.length===1?'':'s');
  $('#page-number').textContent=state.tablePage+' / '+pages;$('#previous-page').disabled=state.tablePage===1;$('#next-page').disabled=state.tablePage>=pages;
}
function renderList() {
  const type=state.page,schema=schemas[type];
  $('#content').innerHTML=heading(pageTitle[type],schema.description,canWrite()?'<button id="new-record" class="primary">+ Novo '+schema.singular+'</button>':'')+
  '<section class="panel"><div class="toolbar"><input class="search" id="search" aria-label="Pesquisar '+type+'" placeholder="Pesquisar '+type+'…"><button class="secondary" id="refresh">Actualizar</button></div>'+
  (type==='utilizadores'?'<div class="tabs">'+['Todos',...Object.keys(labels)].map(role=>'<button data-role="'+role+'" class="tab '+(role==='Todos'?'active':'')+'">'+(labels[role]||role)+'</button>').join('')+'</div>':'')+
  '<div class="table-wrap"><table><thead><tr>'+schema.columns.map(column=>'<th scope="col">'+column+'</th>').join('')+'</tr></thead><tbody id="table-body"></tbody></table></div><div class="table-footer"><span id="row-count"></span><div><button id="previous-page" class="secondary">Anterior</button> <span id="page-number"></span> <button id="next-page" class="secondary">Seguinte</button></div></div></section>';
  updateTable();$('#search').oninput=event=>{state.search=event.target.value;state.tablePage=1;updateTable();};
  $('#refresh').onclick=renderPage;if($('#new-record'))$('#new-record').onclick=()=>openEditor(type);
  document.querySelectorAll('[data-role]').forEach(button=>button.onclick=()=>{state.roleFilter=button.dataset.role;state.tablePage=1;document.querySelectorAll('[data-role]').forEach(tab=>tab.classList.toggle('active',tab===button));updateTable();});
  $('#previous-page').onclick=()=>{state.tablePage--;updateTable();};$('#next-page').onclick=()=>{state.tablePage++;updateTable();};
  $('#table-body').onclick=event=>{
    const button=event.target.closest('[data-action]');if(!button)return;
    const row=state.cache[type].find(item=>item.id===Number(button.dataset.id));if(!row)return;
    if(button.dataset.action==='view')openDetails(type,row);
    if(button.dataset.action==='edit')openEditor(type,row);
    if(button.dataset.action==='delete')confirmMutation(type,row,false);
    if(button.dataset.action==='state')confirmMutation(type,row,true);
  };
}
function openModal(title,html) {$('#modal-title').textContent=title;$('#modal-body').innerHTML=html;if(!$('#modal').open)$('#modal').showModal();}
function openDetails(type,row) {
  let entries;
  if(type==='alunos')entries={Nome:row.nome,Matrícula:row.numeroMatricula,'Data de nascimento':dateText(row.dataNascimento),Turma:row.turma?.codigo,Encarregados:(row.encarregados||[]).map(item=>item.nome).join(', ')||'Nenhum associado'};
  else if(type==='utilizadores')entries={Nome:row.nome,Utilizador:row.username,Perfil:labels[row.role],Estado:row.activo?'Activo':'Inactivo','E-mail':row.email,Telefone:row.telefone};
  else entries=Object.fromEntries(Object.entries(row).filter(([key])=>key!=='id'));
  openModal('Detalhes do '+schemas[type].singular,'<div class="modal-content"><dl class="review-list">'+Object.entries(entries).map(([key,value])=>'<dt>'+escapeHTML(({nome:'Nome',codigo:'Código',classe:'Classe',anoLetivo:'Ano lectivo',turno:'Turno',numeroFuncionario:'N.º funcionário',email:'E-mail',telefone:'Telefone',documento:'Documento'})[key]||key)+'</dt><dd>'+escapeHTML(value||'—')+'</dd>').join('')+'</dl><div class="modal-actions"><button class="secondary" data-close>Fechar</button></div></div>');
  $('[data-close]').onclick=()=>$('#modal').close();
}
function field(name,label,value='',{type='text',required=false,max,min,full=false,options,help}={}) {
  return '<div class="field '+(full?'full':'')+'"><label for="field-'+name+'">'+escapeHTML(label)+(required?' *':'')+'</label>'+
    (options?'<select id="field-'+name+'" name="'+name+'" '+(required?'required':'')+'>'+options.map(([id,text])=>'<option value="'+escapeHTML(id)+'" '+(String(value)===String(id)?'selected':'')+'>'+escapeHTML(text)+'</option>').join('')+'</select>':'<input id="field-'+name+'" name="'+name+'" type="'+type+'" value="'+escapeHTML(value)+'" '+(required?'required ':'')+(max!==undefined?(type==='number'||type==='date'?'max':'maxlength')+'="'+max+'" ':'')+(min!==undefined?(type==='number'?'min':'minlength')+'="'+min+'" ':'')+(type==='password'?'autocomplete="new-password"':'')+'>')+
    (help?'<p class="form-help">'+escapeHTML(help)+'</p>':'')+'</div>';
}
async function openEditor(type,row=null) {
  if(type==='utilizadores'){openUserWizard(row);return;}
  try {
    if(type==='alunos') {
      const generation=state.generation;
      await Promise.all([load('turmas'),load('encarregados')]);
      if(!state.user||generation!==state.generation)return;
      if(!state.cache.turmas.length){toast('Crie primeiro uma turma para poder cadastrar alunos.',true);return;}
    }
    let html;
    if(type==='turmas')html=field('codigo','Código',row?.codigo,{required:true,max:20})+field('classe','Classe',row?.classe,{required:true,max:80})+field('anoLetivo','Ano lectivo',row?.anoLetivo||new Date().getFullYear(),{required:true,type:'number',min:2020,max:2100})+field('turno','Turno',row?.turno,{max:30});
    if(type==='professores')html=field('nome','Nome completo',row?.nome,{required:true,max:120,full:true})+field('numeroFuncionario','N.º de funcionário',row?.numeroFuncionario,{required:true,max:30})+field('telefone','Telefone',row?.telefone,{type:'tel',max:30})+field('email','E-mail',row?.email,{type:'email',max:160,full:true});
    if(type==='encarregados')html=field('nome','Nome completo',row?.nome,{required:true,max:120,full:true})+field('documento','Documento',row?.documento,{required:true,max:40})+field('telefone','Telefone',row?.telefone,{required:true,type:'tel',max:30})+field('email','E-mail',row?.email,{type:'email',max:160,full:true});
    if(type==='alunos') {
      const today=new Date(),localDate=[today.getFullYear(),String(today.getMonth()+1).padStart(2,'0'),String(today.getDate()).padStart(2,'0')].join('-');
      html=field('nome','Nome completo',row?.nome,{required:true,max:120,full:true})+field('numeroMatricula','N.º de matrícula',row?.numeroMatricula,{required:true,max:30})+field('dataNascimento','Data de nascimento',row?.dataNascimento,{type:'date',max:localDate})+field('turmaId','Turma',row?.turma?.id||'',{required:true,full:true,options:[['','Seleccione uma turma'],...state.cache.turmas.map(t=>[t.id,t.codigo+' · '+t.classe+' · '+t.anoLetivo])]})+
      '<div class="field full"><label>Encarregados de educação</label><div class="checkbox-list">'+(state.cache.encarregados.length?state.cache.encarregados.map(e=>'<label><input type="checkbox" name="encarregadoIds" value="'+e.id+'" '+(row?.encarregados?.some(item=>item.id===e.id)?'checked':'')+'>'+escapeHTML(e.nome)+' · '+escapeHTML(e.documento)+'</label>').join(''):'<p class="form-help">Ainda não há encarregados. Pode associá-los mais tarde ao editar o aluno.</p>')+'</div><p class="form-help">Seleccione ou retire selecções para gerir as associações.</p></div>';
    }
    openModal((row?'Editar ':'Novo ')+schemas[type].singular,'<form id="record-form" class="modal-content"><div class="form-grid">'+html+'</div><p class="error" id="form-error" role="alert" hidden></p><div class="modal-actions"><button type="button" class="secondary" data-close>Cancelar</button><button class="primary" type="submit">Guardar</button></div></form>');
    $('[data-close]').onclick=()=>$('#modal').close();
    $('#record-form').onsubmit=async event=>{
      event.preventDefault();const form=event.currentTarget,button=form.querySelector('[type=submit]'),data=Object.fromEntries(new FormData(form));
      if(type==='turmas')data.anoLetivo=Number(data.anoLetivo);
      if(type==='alunos'){data.turmaId=Number(data.turmaId);data.dataNascimento=data.dataNascimento||null;data.encarregadoIds=new FormData(form).getAll('encarregadoIds').map(Number);}
      if('email'in data)data.email=data.email||null;
      button.disabled=true;$('#form-error').hidden=true;
      const generation=state.generation;
      try {await api(endpoint(type)+(row?'/'+row.id:''),{method:row?'PUT':'POST',body:data});if(generation!==state.generation)return;$('#modal').close();toast('Registo guardado com sucesso.');await load(type);if(state.page===type)updateTable();}
      catch(error){if($('#form-error')){$('#form-error').textContent=error.message;$('#form-error').hidden=false;}}
      finally {button.disabled=false;}
    };
  }catch(error){toast(error.message,true);}
}
function openUserWizard(row=null) {
  const data={nome:row?.nome||'',username:row?.username||'',email:row?.email||'',telefone:row?.telefone||'',role:row?.role||'PROFESSOR',password:''};
  let step=0;
  const allowedRoles=state.user.role==='ADMIN'?Object.keys(labels):['PROFESSOR','ALUNO','ENCARREGADO'];
  function paint() {
    let body;
    if(step===0)body='<div class="form-grid">'+field('nome','Nome completo',data.nome,{required:true,max:120,full:true})+field('telefone','Telefone',data.telefone,{type:'tel',max:30})+field('email','E-mail',data.email,{type:'email',max:160})+'</div>';
    if(step===1)body='<div class="form-grid">'+field('username','Nome de utilizador',data.username,{required:true,max:60,full:true})+field('password','Palavra-passe'+(row?' (opcional)':''),data.password,{type:'password',required:!row,min:8,max:72,full:true,help:row?'Deixe vazio para manter a palavra-passe actual.':'Use entre 8 e 72 caracteres.'})+field('role','Perfil',data.role,{required:true,full:true,options:allowedRoles.map(role=>[role,labels[role]])})+'</div>';
    if(step===2)body='<div class="notice"><b>Permissões do perfil '+escapeHTML(labels[data.role])+'.</b><br>'+escapeHTML(permissionText(data.role))+'</div><p class="muted">As permissões são atribuídas pelo perfil e verificadas pelo servidor.</p>';
    if(step===3)body='<dl class="review-list">'+Object.entries({Nome:data.nome,Utilizador:data.username,'E-mail':data.email||'—',Telefone:data.telefone||'—',Perfil:labels[data.role]}).map(([key,value])=>'<dt>'+key+'</dt><dd>'+escapeHTML(value)+'</dd>').join('')+'</dl>';
    openModal((row?'Editar':'Criar')+' utilizador','<form class="modal-content" id="user-form"><div class="wizard-steps">'+['Dados pessoais','Perfil','Permissões','Confirmar'].map((label,index)=>'<span class="wizard-step '+(index===step?'active':'')+'"><b>'+(index+1)+'</b>'+label+'</span>').join('')+'</div>'+body+'<p class="error" id="form-error" role="alert" hidden></p><div class="modal-actions"><button type="button" class="secondary" data-close>Cancelar</button>'+(step?'<button type="button" class="secondary" id="wizard-back">Anterior</button>':'')+'<button class="primary" type="submit">'+(step===3?'Guardar utilizador':'Seguinte →')+'</button></div></form>');
    $('[data-close]').onclick=()=>$('#modal').close();
    if($('#wizard-back'))$('#wizard-back').onclick=()=>{Object.assign(data,Object.fromEntries(new FormData($('#user-form'))));step--;paint();};
    $('#user-form').onsubmit=async event=>{
      event.preventDefault();Object.assign(data,Object.fromEntries(new FormData(event.currentTarget)));
      if(step<3){step++;paint();return;}
      const button=event.currentTarget.querySelector('[type=submit]');button.disabled=true;
      const generation=state.generation;
      try {
        const saved=await api(endpoint('utilizadores')+(row?'/'+row.id:''),{method:row?'PUT':'POST',body:{...data,email:data.email||null,telefone:data.telefone||null}});
        if(generation!==state.generation)return;
        const self=row?.id===state.user.id;
        if(self){state.user=saved;refreshAccount();}
        $('#modal').close();toast('Utilizador guardado com sucesso.');
        if(self&&data.password){logout('Palavra-passe alterada. Entre novamente.');return;}
        await load('utilizadores');if(state.page==='utilizadores')updateTable();
      }catch(error){if($('#form-error')){$('#form-error').textContent=error.message;$('#form-error').hidden=false;}}
      finally {button.disabled=false;}
    };
  }
  paint();
}
function permissionText(role) {
  const text={ADMIN:'Pode gerir utilizadores, cadastrar, editar e apagar registos académicos.',SECRETARIA:'Pode cadastrar e editar dados académicos e criar contas de professor, aluno e encarregado.',DIRECTOR:'Pode consultar turmas, professores, alunos e encarregados.',COORDENADOR:'Pode consultar turmas, professores, alunos e encarregados.',PROFESSOR:'Pode consultar turmas e professores. O acesso individual a turmas e reuniões será acrescentado numa próxima etapa.',ALUNO:'Pode entrar na plataforma e consultar o seu perfil. O acesso individual aos dados académicos está em preparação.',ENCARREGADO:'Pode entrar na plataforma e consultar o seu perfil. O acesso aos dados dos educandos está em preparação.'};
  return text[role];
}
function confirmMutation(type,row,isState) {
  const action=isState?(row.activo?'Desactivar':'Activar'):'Apagar';
  openModal(action+' '+schemas[type].singular,'<div class="modal-content"><p class="muted">'+action+' <strong>'+escapeHTML(row.nome||row.codigo)+'</strong>?</p><p class="form-help">'+(isState?'Uma conta inactiva não pode iniciar sessão.':'A remoção é definitiva. Registos associados podem impedir esta operação.')+'</p><p class="error" id="form-error" role="alert" hidden></p><div class="modal-actions"><button class="secondary" data-close>Cancelar</button><button class="primary" id="confirm-action">'+action+'</button></div></div>');
  $('[data-close]').onclick=()=>$('#modal').close();
  $('#confirm-action').onclick=async event=>{
    const button=event.currentTarget;button.disabled=true;const generation=state.generation;
    try {await api(endpoint(type)+'/'+row.id+(isState?'/estado':''),{method:isState?'PATCH':'DELETE',body:isState?{activo:!row.activo}:undefined});if(generation!==state.generation)return;$('#modal').close();toast('Operação concluída.');await load(type);if(state.page===type)updateTable();}
    catch(error){if($('#form-error')){$('#form-error').textContent=error.message;$('#form-error').hidden=false;}}
    finally{button.disabled=false;}
  };
}
function renderProfile() {
  const u=state.user;
  $('#content').innerHTML=heading('Meu perfil','Informações da sua conta no Colégio Sapiens.')+'<section class="panel"><div class="panel-header"><h2>'+escapeHTML(u.nome)+'</h2><span class="badge">'+escapeHTML(labels[u.role])+'</span></div><div class="panel-body profile-grid">'+Object.entries({'Nome de utilizador':u.username,Perfil:labels[u.role],'E-mail':u.email||'Não indicado',Telefone:u.telefone||'Não indicado'}).map(([label,value])=>'<div class="profile-item"><span>'+label+'</span><b>'+escapeHTML(value)+'</b></div>').join('')+'</div></section><div class="notice" style="margin-top:22px">'+escapeHTML(permissionText(u.role))+'</div>';
}
function renderModule(page) {
  const notice='<div class="notice"><b>Funcionalidade em preparação.</b> Esta área segue o protótipo dos designers. A integração com os serviços necessários ainda não está implementada.</div>';
  if(page==='integracoes')$('#content').innerHTML=heading(pageTitle[page],'Ligação ao sistema académico existente.')+notice+'<section class="panel"><div class="panel-header"><h2>Sistema Académico</h2><span class="badge inactive">Não ligado</span></div><div class="panel-body"><div class="flow"><span>Sistema académico<br>existente</span>→<span>Sapiens</span>→<span>Comunicação</span></div><p class="muted">A ligação externa depende da API e das regras de acesso do sistema académico do colégio.</p></div></section>';
  if(page==='comunicacao')$('#content').innerHTML=heading(pageTitle[page],'Uma comunicação escolar mais próxima das famílias.')+notice+'<div class="module-grid">'+[['Aplicação','Notificações, mensagens e convocatórias.'],['SMS','Alertas e mensagens essenciais.'],['E-mail','Comunicados, relatórios e convocatórias.'],['Telefone','Contacto em situações urgentes.']].map(([title,text])=>'<section class="panel module-card"><span class="stat-icon" aria-hidden="true">✉</span><h3>'+title+'</h3><p>'+text+'</p><span class="badge inactive">Em preparação</span></section>').join('')+'</div>';
  if(page==='reunioes')$('#content').innerHTML=heading('Reuniões','Solicitações e convocatórias para os encarregados de educação.')+notice+'<section class="panel empty"><h3>As reuniões vão aparecer aqui</h3><p>O fluxo de escolha do aluno, motivo, detalhes, data e confirmação será ligado à API numa próxima etapa.</p></section>';
}
function renderReports() {
  const types=['turmas','alunos','professores','encarregados'].filter(readable);
  $('#content').innerHTML=heading('Relatórios','Resumo dos cadastros disponíveis para o seu perfil.')+(types.length?'<div class="cards">'+types.map(type=>'<div class="stat-card"><div class="stat-top">'+pageTitle[type]+'</div><div class="stat-value">'+state.cache[type].length+'</div><div class="stat-caption">Registos cadastrados</div></div>').join('')+'</div><section class="panel"><div class="panel-body"><p class="muted">Exporte os cadastros para um ficheiro CSV.</p>'+types.map(type=>'<button class="secondary" data-export="'+type+'" style="margin:4px">Exportar '+type+'</button>').join('')+'</div></section>':'<div class="notice">O seu perfil ainda não dispõe de relatórios académicos.</div>');
  document.querySelectorAll('[data-export]').forEach(button=>button.onclick=()=>exportCSV(button.dataset.export));
}
function exportCSV(type) {
  const columns={turmas:['id','codigo','classe','anoLetivo','turno'],alunos:['id','nome','numeroMatricula','dataNascimento','turma','encarregados'],professores:['id','nome','numeroFuncionario','email','telefone'],encarregados:['id','nome','documento','telefone','email']}[type];
  const cell=value=>{let string=String(value??'');if(/^[=+@\-\t\r]/.test(string))string="'"+string;return '"'+string.replace(/"/g,'""')+'"';};
  const rows=state.cache[type].map(row=>columns.map(column=>cell(column==='turma'?row.turma?.codigo:column==='encarregados'?row.encarregados?.map(e=>e.nome).join(', '):row[column])).join(';'));
  const url=URL.createObjectURL(new Blob(['\ufeff'+columns.join(';')+'\r\n'+rows.join('\r\n')],{type:'text/csv;charset=utf-8'}));
  const link=document.createElement('a');link.href=url;link.download='sapiens-'+type+'.csv';link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
}
function refreshAccount() {
  $('#account-label').textContent=state.user.nome;$('#avatar').textContent=initials(state.user.nome);$('#footer-role').textContent=labels[state.user.role];
}
$('#login-form').onsubmit=async event=>{
  event.preventDefault();const form=event.currentTarget,button=form.querySelector('[type=submit]'),username=form.username.value.trim(),password=form.password.value;
  button.disabled=true;$('#login-error').hidden=true;
  try {
    if(username.includes(':'))throw new Error('O nome de utilizador não pode conter dois-pontos.');
    const user=await api('/api/auth/login',{method:'POST',body:{username,password},authorization:null});
    state.user=user;state.authorization='Basic '+btoa(String.fromCharCode(...new TextEncoder().encode(username+':'+password)));
    state.generation++;form.password.value='';refreshAccount();$('#login-screen').hidden=true;$('#app-screen').hidden=false;
    await renderPage();
  }catch(error){$('#login-error').textContent=error.message;$('#login-error').hidden=false;}
  finally{button.disabled=false;}
};
$('#toggle-password').onclick=()=>{const input=$('#login-password'),show=input.type==='password';input.type=show?'text':'password';$('#toggle-password').textContent=show?'Ocultar':'Mostrar';$('#toggle-password').setAttribute('aria-label',show?'Ocultar palavra-passe':'Mostrar palavra-passe');};
$('#forgot-password').onclick=()=>$('#recovery-help').hidden=!$('#recovery-help').hidden;
$('#logout-button').onclick=()=>logout();
$('#profile-button').onclick=()=>{if(location.hash==='#perfil')renderPage();else location.hash='perfil';};
$('#menu-toggle').onclick=()=>$('.sidebar').classList.toggle('open');
$('#close-modal').onclick=()=>$('#modal').close();
$('#modal').addEventListener('close',()=>$('#modal-body').replaceChildren());
window.addEventListener('hashchange',renderPage);
