//Bibliotecas JADE para multiagentes

import jade.core.Agent;
import jade.core.AID;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import jade.proto.AchieveREResponder;
import jade.proto.ContractNetResponder;
import jade.proto.SubscriptionInitiator;
import jade.proto.ContractNetInitiator;
import jade.proto.SubscriptionResponder;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.FailureException;
//Bibliotecas JADE para comportamentos temporais
import jade.core.behaviours.Behaviour;
//import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.util.leap.ArrayList;

import java.io.File;
//Bibliotecas para lidar com arquivos XML
import java.io.IOException;
import java.lang.*;
import java.lang.reflect.UndeclaredThrowableException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
//import org.omg.CORBA.portable.UnknownException;

public class agenteAlimentador extends Agent {

	double cargaPerdida, cargaTotalDisponivelMicrorrede,cargaTotalPerdida = 0;
	
	public void setup(){
		//Pega o nome do agente no Banco de Dados
		final String agenteAlimentador = getLocalName();
		final Element agenteALBD = carregaBD(agenteAlimentador);
	
		final MessageTemplate filtroAtuacao = MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE),
		  		MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE));
		
		//Filtro contract net para receber solicitações de outros ALs
		final MessageTemplate filtroContractNet = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
				MessageTemplate.MatchPerformative(ACLMessage.CFP) );
		
		
		
		/**
		 * M�todo para receber os valores atuais de carga
		 */		
		/*
		addBehaviour(new TickerBehaviour(this,100) {
			public void onTick(){
				//ACLMessage filtroAtualizacao = receive();
				//if(filtroAtualizacao != null){
				
				ACLMessage filtroRotina = receive();
				if(filtroRotina != null){
				
					//exibirMensagem(agree);
					
					String agenteChave = filtroRotina.getSender().getLocalName();
					System.out.println("-<<"+agenteAlimentador+">>: recebi um valor de carga de "+agenteChave);
					String cargaDemandada = filtroRotina.getContent();
					agenteAlimentadorBD.getChild("chaves").getChild(agenteChave).setAttribute("carga",cargaDemandada); //Atualiza o seu banco de dados
				
				}
			}
		});
		*/
		/********************************************************************************************************************** 
		 *Parte do FIPA Subscribe participante para receeber informação de curto ou carga
		 **********************************************************************************************************************/
		addBehaviour(new SubscriptionResponder(this, filtroAtuacao) {
			private static final long serialVersionUID = 1L;

			protected ACLMessage handleSubscription(ACLMessage subscription){
				ACLMessage resposta = subscription.createReply();
				resposta.setContent("ok");
				resposta.setPerformative(ACLMessage.AGREE); 
				
				String conteudo = subscription.getContent();
				
				if(conteudo.equals("falta")){
					exibirAviso(myAgent, "Fui informado de uma falta ###################################################################################################################################################");
										
					//Atualizar  o XML para saber onde foi a falta
					String agenteChaveSobFalta = subscription.getSender().getLocalName();
					agenteALBD.getChild("chaves").getChild(agenteChaveSobFalta).setAttribute("atuacao","sim");
					
					String referencia = agenteChaveSobFalta.split("_")[1]; //500e3/Só para pegar o número do agente chave para avisar somente os ajusante
//					exibirAviso(myAgent, "A referência do agente chave é "+referencia);
					
//					String referenciaDaChave = referencia.split("R")[1];
					final int referenciaDaChaveAtuante =Integer.parseInt(referencia.split("R")[1]); //Só para pegar o número do agente chave para avisar somente os ajusante
					exibirAviso(myAgent, "A chave atuante é: "+referenciaDaChaveAtuante);
					
					//*********Saber se tem agente chave e quantos são
					List lista = agenteALBD.getChild("chaves").getChildren(); 
					Iterator i = lista.iterator();
					
					int cont = 0; //inicia cont com zero
					
				    while(i.hasNext()) { 
				    	Element elemento = (Element) i.next();
				    	String nome = String.valueOf(elemento.getName());
				    	
				    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes chave no XML, então add ele como remetente
				    		cont = cont + 1; //Se houver algum agente chave, incrementa o contador
				    	}
				    }
//				    if(cont!=0){ // Se existirem agentes chave
				    if(cont>referenciaDaChaveAtuante){ //Se o número de agente chave for maior que o índice da chave atuante, é porque com certeza há chaves a jusante da chave atuante
//						exibirAviso(myAgent, "Há chaves a jusante da que sentiu o curto.");
				    	/**********************************************************************************
					     * Protocolo FIPA Request para solicitar que todos os ACs abram suas chaves
					     * 
					     *********************************************************************************/
				  		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
				  		msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
				  		msg.setContent("abra");
				  		
				  		//Cria-se uma lista para percorrer a tag CHAVES (agentes chave)
				  		List lista1 = agenteALBD.getChild("chaves").getChildren(); 
						Iterator i1 = lista1.iterator();
											
					    while(i1.hasNext()) { 
					    	Element elemento = (Element) i1.next();
					    	String nome = String.valueOf(elemento.getName());
					    	
//							exibirAviso(myAgent, "Analisando se aviso ao agente chave "+nome+" que comande a abertura de seu religador.");
							
					    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes chave no XML, então add ele como remetente
					    		int referenciaDaChave = Integer.parseInt(nome.split("_")[1].split("R")[1]); //Analisa-se a posição da chave visada
					    		
//					    		exibirAviso(myAgent, "A referência de "+nome+" é "+referenciaDaChave);
					    		
					    		if(referenciaDaChave>referenciaDaChaveAtuante){ //Se a chave analisada estiver localizada a jusante da chave atuante então
//					    			exibirAviso(myAgent, "A referência de "+nome+" é maior que a da chave atuante que é "+referenciaDaChaveAtuante);
					    			
					    			if(elemento.getAttributeValue("atuacao").equals("nao")){ //Se o agente chave não é o que atuou então vou mandar uma mensagem pra ele
						    			
					    				exibirAviso(myAgent, "Solicitando ao agente chave "+nome+" que comande a abertura de sua chave.");
//					    				
							    		msg.addReceiver(new AID((String) nome, AID.ISLOCALNAME));
							    		/*
							    		 * Aqui eu já sei a chave que abrirá e já pego é o valor de carga perdida dela no XML
							    		 * Na verdade, basta pegar o valor de corrente medida no trecho a jusante ao do trecho em falta. Ele já me dirá
							    		 * toda a carga perdida
							    		 */
							    		if(referenciaDaChave == referenciaDaChaveAtuante + 1){
							    			cargaPerdida = Double.parseDouble(elemento.getAttributeValue("carga"));
							    			exibirAviso(myAgent, "O agente chave "+nome+" o a jusante do em falta. Vou pegar seu valor de carga: "+cargaPerdida );
							    			//##################################################################################################################################################################
//								    		cargaTotalPerdida = cargaTotalPerdida + cargaPerdida;
//							    			cargaTotalPerdida = cargaPerdida;
							    		}
							    		
//							    		cargaPerdida = 0; //Acho que nem precisa disso não
					    			}// Fim do if para saber se atuou ou não
					    		}// Fim do if para saber se referenciaDaChave>referenciaDaChaveAtuante
					    	}// Fim do if para saber se há chave
					    }// Fim do while(i.hasNext())	
				  		
				  		
					    addBehaviour(new AchieveREInitiator(myAgent, msg) {
							protected void handleInform(ACLMessage inform) {
								System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
							
								
							}
							protected void handleRefuse(ACLMessage refuse) {
//								System.out.println("Agent "+refuse.getSender().getName()+" refused to perform the requested action");
//								nResponders--;
							}
							protected void handleFailure(ACLMessage failure) {
								if (failure.getSender().equals(myAgent.getAMS())) {
									// FAILURE notification from the JADE runtime: the receiver
									// does not exist
									System.out.println("Responder does not exist");
								}
								else {
									System.out.println("Agent "+failure.getSender().getName()+" failed to perform the requested action");
								}
							}
							protected void handleAllResultNotifications(Vector notifications) {
//								if (notifications.size() < nResponders) {
//									// Some responder didn't reply within the specified timeout
//									System.out.println("Timeout expired: missing "+(nResponders - notifications.size())+" responses");
//								}
							}
						}); //Fim do addBehcaviour do Request para avisar os agentes chave que abram
				  		
					    
					  //*********Já sei que há trechos a jusante (consequentemente agentes chave, agora, saber se tem micrrodes nos trechos a jusante.
						exibirAviso(myAgent, "Saber se tem microrredes para serem avisadas e ilharem.");
					    List lista2 = agenteALBD.getChild("microrredes").getChildren(); //Vê todos agente chave para ver se no trechos deles tem microrrede
						Iterator i2 = lista2.iterator();
						
						int contMicrorredes = 0; //inicia cont com zero
						
					    while(i2.hasNext()) { 
					    	Element elemento = (Element) i2.next();
					    	String nome = String.valueOf(elemento.getName());
					    	
					    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes chave no XML, então add ele como remetente
//					    		exibirAviso(myAgent, "Vamos analisar se analiso a existência de microrredes no mesmo trecho do AC "+nome+". A microrrde tem que está no mesmo trech do curto ou depois");
					    		//Tenho que analisar o nome do agente chave para começar a avisar a microrredes a partir da que pertence ao mesmo trecho do AC  que atuou por curto
					    		int referenciaACAnalisado = Integer.parseInt(nome.split("_")[1].split("R")[1]);
					    		
					    		if(referenciaACAnalisado>=referenciaDaChaveAtuante){
//					    			exibirAviso(myAgent, nome+" está a jusante do da chave que atuou ou é a chave que atuou. Vamos analisar se há microrrede no trecho do agente chave "+nome);
						    		//Estou na TAG nome do agente chave. Vou olhar se no trecho dele tem microrredes. 
						    		List lista3 = agenteALBD.getChild("microrredes").getChild(nome).getChildren(); //Vê todos agente chave para ver se no trechos deles tem microrrede
									Iterator i3 = lista3.iterator();
									
								    while(i3.hasNext()) { 
								    	Element elemento2 = (Element) i3.next();
								    	String nomeMicrorrede = String.valueOf(elemento2.getName());
//								    	exibirAviso(myAgent, "Era pra ter aqui o nome nenhum ou a referência de um apc. No caso, tá dando: "+nomeMicrorrede);
								    	
								    	if (nomeMicrorrede!= null && nomeMicrorrede.length()>0 && nomeMicrorrede!= "nenhum"){ //Se houver agentes chave no XML, então add ele como remetente
								    		contMicrorredes = contMicrorredes + 1; //Se houver microrrede, incrementa o contador
								    	}// Fim do if para saber se tem microrredes de fato, (se o nome é diferente de nenhum)
								    }//Fim do while(i3.hasNext()) para percorrer as microrredes dentro da tag do agente chave
					    		}// Fim do if comparando a referências das chaves para avisar as microrredes do trecho afetado em diante
					    	}// Fim do if para saber se tem agente chave (se o nome é diferente de nenhum)
					    }//Fim do while(i3.hasNext()) para percorrer o nome das microrredes
					    
					    if(contMicrorredes>0){ //Se existirem microrredes para serem avisadas
					    	exibirAviso(myAgent, "Há microrredes para serem alertadas de modo que ilhem.");
					    	/**********************************************************************************
						     * Protocolo FIPA Request para solicitar que APCs abram suas chaves
						     * 
						     *********************************************************************************/
					  		ACLMessage msg2 = new ACLMessage(ACLMessage.REQUEST);
					  		msg2.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
					  		msg2.setContent("abra");
					  		
					  		//Cria-se uma lista para percorrer a tag CHAVES (agentes chave)
					  		List lista3 = agenteALBD.getChild("microrredes").getChildren(); 
							Iterator i3 = lista3.iterator();
							
							while(i3.hasNext()) { 
						    	Element elemento = (Element) i3.next();
						    	String nome = String.valueOf(elemento.getName()); // nome da chave
						    	
						    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes chave no XML, então add ele como remetente
						    	
						    		//Tenho que analisar o nome do agente chave para começar a avisar a microrredes a partir da que pertence ao mesmo trecho do AC  que atuou por curto
						    		int referenciaACAnalisado = Integer.parseInt(nome.split("_")[1].split("R")[1]);
						    		
						    		if(referenciaACAnalisado>=referenciaDaChaveAtuante){
						    		
							    		//Estou na TAG nome do agente chave. Vou olhar se no trecho dele tem microrredes. 
							    		List lista4 = agenteALBD.getChild("microrredes").getChild(nome).getChildren(); //Vê todos agente chave para ver se no trechos deles tem microrrede
										Iterator i4 = lista4.iterator();
										
									    while(i4.hasNext()) { 
									    	Element elemento4 = (Element) i4.next();
									    	String nomeMicrorrede = String.valueOf(elemento4.getName());
									    	
									    	if (nomeMicrorrede!= null && nomeMicrorrede.length()>0 && nomeMicrorrede!= "nenhum"){ //Se houver agentes chave no XML, então add ele como remetente
									    		exibirAviso(myAgent, "Vou avisar a microrrde "+nomeMicrorrede+" que ilhe!!!!!");
									    		msg2.addReceiver(new AID((String) nomeMicrorrede, AID.ISLOCALNAME));
									    		
									    		if(referenciaACAnalisado>referenciaDaChaveAtuante){//Aqui é só para pegar o valor de potência disponível das microrredes a jusante do trecho em falta
									    			/*
										    		 * Aqui eu já sei a microrrede que está a jusante do trecho afetado e que pode disponibilizar carga
										    		 */
//										    		double cargaDisponivelMicrorrede = Double.parseDouble(elemento4.getAttributeValue("carga"));
									    			double cargaDisponivelMicrorrede = Double.parseDouble(agenteALBD.getChild("microrredes").getChild(nome).getChild(nomeMicrorrede).getAttributeValue("potenciaDisponivel"));
										    		
									    			
									    			cargaTotalDisponivelMicrorrede = cargaTotalDisponivelMicrorrede + cargaDisponivelMicrorrede;
									    		}
									    	}// Fim do if para saber se tem microrredes de fato, (se o nome é diferente de nenhum)
									    }//Fim do while(i3.hasNext()) para percorrer as microrredes dentro da tag do agente chave
						    		}// Fim do if comparando a referências das chaves para avisar as microrredes do trecho afetado em diante
						    	}// Fim do if para saber se tem agente chave (se o nome é diferente de nenhum)
						    }//Fim do while(i3.hasNext()) para percorrer o nome das microrredes
					   
					  		
						    addBehaviour(new AchieveREInitiator(myAgent, msg2) {
								protected void handleInform(ACLMessage inform) {
									System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
								
									
								}
								protected void handleRefuse(ACLMessage refuse) {
//									System.out.println("Agent "+refuse.getSender().getName()+" refused to perform the requested action");
//									nResponders--;
								}
								protected void handleFailure(ACLMessage failure) {
									if (failure.getSender().equals(myAgent.getAMS())) {
										// FAILURE notification from the JADE runtime: the receiver
										// does not exist
										System.out.println("Responder does not exist");
									}
									else {
										System.out.println("Agent "+failure.getSender().getName()+" failed to perform the requested action");
									}
								}
								protected void handleAllResultNotifications(Vector notifications) {
//									if (notifications.size() < nResponders) {
//										// Some responder didn't reply within the specified timeout
//										System.out.println("Timeout expired: missing "+(nResponders - notifications.size())+" responses");
//									}
								}
							}); //Fim do addBehcaviour do Request para avisar os agentes chave que abram
					    }
					
					    /*Aqui eu vou calcular o valor de potencia perdida consultando o XML. 
					     * Se preciso, coloco aquele comportamento que faz o agente dormir um pouco para depois executar a instrução desejada
					     * */
					    addBehaviour(new WakerBehaviour(myAgent, 1000) {
					    	protected void handleElapsedTimeout() {
					    		
					    		
					    		 //Vou percorrer a tag "chaves" no XML
							    //Vou pegando o valor de carga de cada chave
							    //Vou somando dentro do while
							    
							    //vou percorrer a tag "microrredes" pegando o valor de cada potencia disponivel das microrredes
							    //vou somando cada potência disponível de cada microrrede
							    
							    //Pensando bem, acho que dá pra fazer isso no método anterior. Se eu não fizer isso nas rotinas anteriores, vou fazer semelhante a elas de qualquer forma.
							    /*********************************************************************************
							     * Aqui é porque eu tenho certeza que há outros ALs, se não eu tenho que verificar a quantidade antes
								 * FIPA Contract Net Initiator para negociação 
								*********************************************************************************/
							    exibirAviso(myAgent, "O valor da carga perdida dos trechos é: "+cargaPerdida+". O valor das microrredes são: "+cargaTotalDisponivelMicrorrede);
							    cargaTotalPerdida = cargaPerdida + cargaTotalDisponivelMicrorrede; 
							    //Tenho que zerar esse valor depois
							    exibirAviso(myAgent, " Desse modo, o valor de carga total perdida é: "+cargaTotalPerdida);
							    
							    
							    final ACLMessage negociarDeltaP = new ACLMessage(ACLMessage.CFP);
								List lista3 = agenteALBD.getChild("outrosALs").getChildren(); 
								Iterator i3 = lista3.iterator();
								
							    while(i3.hasNext()) {
							    	Element elemento = (Element) i3.next();
							    	String nome = String.valueOf(elemento.getName());
									
							    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes geradores no XML, então add ele como remetente
//																	System.out.println("Entrou no if!!!!!");  //Só pra testar
							    		//Vou analisar quem é a chave fronteira com esse AL. Se o curto
							    		String chaveFronteira = elemento.getAttributeValue("chaveFronteira");
							    		int refChaveFronteira = Integer.parseInt(chaveFronteira.split("_")[1].split("R")[1]);
							    		
							    		if(refChaveFronteira>referenciaDaChaveAtuante){
							    			exibirAviso(myAgent, "Solicitando a " +nome+ " um valor de carga perdida igual à: "+cargaTotalPerdida);
								    		negociarDeltaP.addReceiver(new AID((String) nome, AID.ISLOCALNAME));
							    		}
							    	}
							    }	
							    
							    negociarDeltaP.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
							    negociarDeltaP.setContent(String.valueOf(cargaTotalPerdida)); 
							    
							    addBehaviour(new ContractNetInitiator(myAgent, negociarDeltaP) {
									
									/**
									 * 
									 */
									private static final long serialVersionUID = 1L;

									protected void handlePropose(ACLMessage propose, Vector v) {
//										exibirMensagem(propose);
						
//											ACLMessage reply = propose.createReply();
//											reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
//											v.addElement(reply);]
									}
									
									protected void handleRefuse(ACLMessage refuse) {
										exibirAviso(myAgent, "Negociação recusada por " + refuse.getSender().getLocalName());
										
////														System.out.println(getLocalName() + ": Enviando resposta para " + negocia.getSender().getLocalName());
//															
//															ACLMessage aviso = negociarDeltaP.createReply();
//															aviso.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
//															aviso.setPerformative(ACLMessage.REFUSE);
//															aviso.setContent("0");
//															comando
//															myAgent.send(aviso);
									}
									
									protected void handleFailure(ACLMessage failure) {
										if (failure.getSender().equals(myAgent.getAMS())) {
											// Notificação de erro feita pela plataforma
											
										}
										else {
//											exibirAviso(myAgent, "-<<"+nomeAgente+">>: o agente "+failure.getSender().getLocalName()+" falhou");
										}										
									} //Fim do handleFailure
									
									protected void handleAllResponses(Vector responses, Vector acceptances) {

										// Evaluate proposals.
										double bestProposal = -1;
										AID bestProposer = null;
										ACLMessage accept = null;
										
										Enumeration e = responses.elements();  //a variável "e" será uma espécie de vetor de elementos, onde os elementos serão as mensagens recebidas
										while (e.hasMoreElements()) { //um while só pra percorrer todas as posições do vetor "e"
											ACLMessage msgProposta = (ACLMessage) e.nextElement();  //a variável msg receberá a cada iteração uma mensagem recebida que corresponde a cada posição de "e"
											
											if (msgProposta.getPerformative() == ACLMessage.PROPOSE){ //Se a performativa da mensagem msg for uma proposta (PROPOSE), então entra no SE
//												ACLMessage resposta = msgProposta.createReply(); //será criada então uma resposta para essa mensagem
//												acceptances.addElement(resposta);
												
												double proposal = Double.parseDouble(msgProposta.getContent()); //Aqui ele pega a propostas para avaliá-la
												if (proposal > bestProposal){
													bestProposal = proposal;
													bestProposer = msgProposta.getSender();
																		
//													resposta.setPerformative(ACLMessage.ACCEPT_PROPOSAL); // seta a performativa logo como Reject_PROPOSAL
//													resposta.setContent(String.valueOf(bestProposal));
//																		accept = resposta;
//																		accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
												}else{
//													resposta.setPerformative(ACLMessage.REJECT_PROPOSAL); // seta a performativa logo como Reject_PROPOSAL
//													resposta.setContent(String.valueOf(proposal));
												}
											}//Fim do if msg.getPErformative() == ACLMessage.PROPOSE
										}//Fim do while (e.hasMoreElements()) 
										
										Enumeration e1 = responses.elements();  //a variável "e" será uma espécie de vetor de elementos, onde os elementos serão as mensagens recebidas
										while (e1.hasMoreElements()) { //um while só pra percorrer todas as posições do vetor "e"
											ACLMessage msgProposta = (ACLMessage) e1.nextElement();  //a variável msg receberá a cada iteração uma mensagem recebida que corresponde a cada posição de "e"
											
											if (msgProposta.getPerformative() == ACLMessage.PROPOSE){ //Se a performativa da mensagem msg for uma proposta (PROPOSE), então entra no SE
												ACLMessage resposta = msgProposta.createReply(); //será criada então uma resposta para essa mensagem
												acceptances.addElement(resposta);
												
												double proposal = Double.parseDouble(msgProposta.getContent()); //Aqui ele pega a propostas para avaliá-la
												if (proposal == bestProposal){
													bestProposal = proposal;
													bestProposer = msgProposta.getSender();
																		
													resposta.setPerformative(ACLMessage.ACCEPT_PROPOSAL); // seta a performativa logo como Reject_PROPOSAL
													resposta.setContent(String.valueOf(bestProposal));
//																		accept = resposta;
//																		accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
													
												}else{
													resposta.setPerformative(ACLMessage.REJECT_PROPOSAL); // seta a performativa logo como Reject_PROPOSAL
													resposta.setContent(String.valueOf(proposal));
												}
											}//Fim do if msg.getPErformative() == ACLMessage.PROPOSE
										}//Fim do while (e.hasMoreElements()) 
										
										
									}// Fim do handle all responses
									
									protected void handleInform(ACLMessage inform) {
//										System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
										//Quando receber o "inform" começa a resomposição. O AL solicitante ou initiator começa então a as manobras
										//Olho então quem foi mesmo o AL o qual foi aceito a proposto e olho no XML qual chave de encontro tenho que fechar
										//Mas aqui eu já tenho que saber também que agente chaves têm que serem fechados
										
										/*
										 * Antes de tudo isso, eu analiso se a melhor proposta pode suprir tudo
										 * Se puder, vejo qual é o sender da mensagem que será o AL que irá ajudar e vejo que chave de encontro tenho que fechar
										 * 
										 * Se não puder ajudar com tudo, tenho que analisar trecho por trecho
										 * Fao um while para ver quantas chaves são
										 * Começo a analisar pelo trecho mais a jusante
										 * Vejo potência perdida nesse trecho
										 * 
										 * potencia disponivel = getContent(propose)
										 * if potencia disponivel do outro AL > Potencia perdida no trecho (i)  (se pelo menos isso)
										 * 		potencia disponviel = potencia disponivel - potencia perdida no trecho (i)  (fica isso para o próximo trecho)
										 * senão if Tem microrrede no trecho (i) 
										 * 		if potencia disponivel + potencia microrrede (i) > potencia perdida no trecho (i)   (E com a ajuda dessa microrrede?
										 * 			Ai ajuda até aqui, pois se não fosse a micrrorede não dava nem pra suprir esse trecho... imagine mais outro(s)
										 * end		
										 * 		
										 * 
										 * 	
										 */
										exibirAviso(myAgent, "O valor de carga total perdida dentro do handle inform é: "+cargaTotalPerdida);
										
										String ALParticipante = inform.getSender().getLocalName();
										double cargaDiponivel = Double.parseDouble(inform.getContent());
										
										exibirAviso(myAgent, "Eu aceitei a proposta de "+ALParticipante+", e a carga que ele se dispôs a dar é "+cargaDiponivel+" sendo que preciso de "+cargaTotalPerdida);
									
										if(cargaDiponivel>=cargaTotalPerdida){ //Se puder ser suprido tudo por outro alimentador, então
											exibirAviso(myAgent, "A carga disponível "+cargaDiponivel+" é maior que "+cargaTotalPerdida);
											
											String chaveDeEncontro = agenteALBD.getChild("outrosALs").getChild(ALParticipante).getAttributeValue("chaveDeEncontro");
											exibirAviso(myAgent, " Será solicitado que a chave de encontro "+chaveDeEncontro+" que feche.");
											/**********************************************************************************
										     * Protocolo FIPA Request para solicitar que a chave de encontro de alimentadores feche
										     * 
										     *********************************************************************************/
									  		ACLMessage msg1 = new ACLMessage(ACLMessage.REQUEST);
									  		msg1.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
									  		
									  		//Cria-se uma lista para percorrer a tag chaves para eu poder avisar os agentes de chave para fechar
									  		List lista2 = agenteALBD.getChild("chaves").getChildren();
											Iterator i2 = lista2.iterator();
											
										    while(i2.hasNext()) { 
										    	Element elemento2 = (Element) i2.next();
										    	String nome2 = String.valueOf(elemento2.getName());  //nome do agente chave
										    	
												exibirAviso(myAgent, "A referencia da chave atuante é: "+referenciaDaChaveAtuante+". Vou ver se aviso a chave "+nome2+" para fechar.");
												
										    	if (nome2!= null && nome2.length()>0 && nome2!= "nenhum"){ //Se houver agentes chave no XML
										    		int referenciaDaChave = Integer.parseInt(nome2.split("_")[1].split("R")[1]); //Analisa-se a posição da chave visada para análise se tem microrrede no mesmo trecho dele
										    		
										    		if(referenciaDaChave>=referenciaDaChaveAtuante + 2){ //Vou analisar a carga das microrredes a jusante do trehcho onde ocorreu a falta
										    			msg1.addReceiver(new AID((String) nome2, AID.ISLOCALNAME));	
									
										    		}//fim do if
										    	}//fim do if
										    }//fim do while
									  		
									  		
									  		msg1.addReceiver(new AID((String) chaveDeEncontro, AID.ISLOCALNAME));	
									  	
										    msg1.setContent("fechar");
									  		
									  		addBehaviour(new AchieveREInitiator(myAgent, msg1) {
												protected void handleInform(ACLMessage inform) {
													System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
												}
												protected void handleRefuse(ACLMessage refuse) {
						//							System.out.println("Agent "+refuse.getSender().getName()+" refused to perform the requested action");
						//							nResponders--;
												}
												protected void handleFailure(ACLMessage failure) {
													if (failure.getSender().equals(myAgent.getAMS())) {
														// FAILURE notification from the JADE runtime: the receiver
														// does not exist
														System.out.println("Responder does not exist");
													}
													else {
														System.out.println("Agent "+failure.getSender().getName()+" failed to perform the requested action");
													}
												}
												protected void handleAllResultNotifications(Vector notifications) {
						//							if (notifications.size() < nResponders) {
						//								// Some responder didn't reply within the specified timeout
						//								System.out.println("Timeout expired: missing "+(nResponders - notifications.size())+" responses");
						//							}
												}
											}); //Fim do addBehaviour do request initiator
											
										}//if(cargaDiponivel>cargaTotalPerdida)
										else{//Se não for possível outro alimentador suprir tudo, então tem-se que analisar tirando trechos
											exibirAviso(myAgent, "Não é possível suprir todos os trechos. Vou analisar trecho a trecho para ver se eh possivel suprir pelo menos uma parte.");
																		
										  //Cria-se uma lista para percorrer a tag CHAVES (agentes chave) para ser feita uma análise de quantos trechos podem ser atendidos com e sem microrrede
									  		List lista = agenteALBD.getChild("chaves").getChildren(); 
											Iterator i = lista.iterator();
//											int cont = 0;					
											
											int refFechar = 0; //referência do agente chave que deve fechar assim 
											int contChaveDeEcontro = 0; //Para saber se o alimentador necessitado tem como ser ajudado de alguma forma %%%%%%%%%%%%%%%%%%%%%%%%%%
											
//											int refMicroRedeFechar = 0; //referência do agente apc que deve fechar assim 
											exibirAviso(myAgent, "refFechar é: "+refFechar);
											
										    while(i.hasNext()) { 
										    	Element elemento = (Element) i.next();
										    	String nome = String.valueOf(elemento.getName());
										    	
																						
										    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes chave no XML, então add ele como remetente
										    		int referenciaDaChave = Integer.parseInt(nome.split("_")[1].split("R")[1]); //Analisa-se a posição da chave visada
										    		
//										    		exibirAviso(myAgent, "A referência de "+nome+" é "+referenciaDaChave);
										    		
										    		if(referenciaDaChave>referenciaDaChaveAtuante){ //Se a chave analisada estiver localizada a jusante da chave atuante então
//										    			exibirAviso(myAgent, "A referência de "+nome+" é maior que a da chave atuante que é "+referenciaDaChaveAtuante);
										    			
										    			//***************************************DADOS DESSE TRECHO*****************************************************
										    			double cargaTrecho = Double.parseDouble(elemento.getAttributeValue("carga")); //Saber a carga pré-falta só desse trecho
										    			
//										    			//Essa parte é só pra saber a contribuição só dessa microrrede (cargaDisponviel + boost), pois pode eu quero cortar a possibilidade de atender o trecho que ela estiver conectada ai tiro o valor dela dos cálculos
//										    			double cargaMicrorredeTrecho = 0;
//										    			//Quero saber a carga só daS microrredeS conectada a esse trecho
//												  		List lista4 = agenteALBD.getChild("microrredes").getChild(nome).getChildren(); 
//														Iterator i4 = lista4.iterator();
//
//														 while(i4.hasNext()) { 
//														    	Element elemento4 = (Element) i4.next();
//														    	String nome4 = String.valueOf(elemento4.getName());
//														    	
////																exibirAviso(myAgent, "Analisando se aviso ao agente chave "+nome+" que comande o fechamento de seu religador.");
//																
//														    	if (nome4!= null && nome4.length()>0 && nome4!= "nenhum"){ //Se houver agentes chave no XML, então add ele como remetente
//														    		cargaMicrorredeTrecho = Double.parseDouble(elemento.getAttributeValue("potenciaDisponivel")) + Double.parseDouble(elemento.getAttributeValue("boost"));
//														    		
//														    	}
//														 }//********************************************************************************************
														 exibirAviso(myAgent, "Não é possível suprir tudo. A carga de "+nome+"é "+cargaTrecho);
										    			/*<<<<<<<<<<<<<<<<<<<<<<<<<
														 if(cargaDiponivel > cargaTotalPerdida){ //Na primeira iteração nunca será possível pois se já estamos aqui é porque não foi mesmo possível outro AL suprir tudo
										    				refFechar = referenciaDaChave + 1; //Tenho que fechar os religadores até essa referência    
										    				exibirAviso(myAgent, "A carga disponível é >"+cargaDiponivel+" e a carga total perdida é >"+cargaTotalPerdida+". No caso cargaDisponivel > cargaTotalPerdida");
//										    			}else if(cargaDiponivel > cargaTotalPerdida - cargaTotalDisponivelMicrorrede){ //então olha-se a contribuição de microrredes
//										    				refFechar = referenciaDaChave + 1;
//										    				refMicroRedeFechar = referenciaDaChave; //as microrredes daquele trecho em diante irão fechar
										    			}else{ //Se mesmo com microrredes não for possível
										    				cargaTotalPerdida = cargaTotalPerdida - cargaTrecho;
//										    				cargaTotalDisponivelMicrorrede = cargaTotalDisponivelMicrorrede - cargaMicrorredeTrecho; //Como esse trecho terá que sair a microrrede sai tbm										    			}//Ai vai pra próxima iteração
										    				exibirAviso(myAgent, "O valor de carga total perdida foi atualizado (no caso diminuido) e agora é: "+cargaTotalPerdida);
										    			}// Fim do if para saber se referenciaDaChave>referenciaDaChaveAtuante
														 >>>>>>>>>>>>>>>>>>>>>>>>>>*/
														 if(cargaDiponivel >= cargaTrecho){ //Na primeira iteração nunca será possível pois se já estamos aqui é porque não foi mesmo possível outro AL suprir tudo
//											    				refFechar = referenciaDaChave + 1; //Tenho que fechar os religadores até a jusante dessa referência. Se for referenciaDaChave = 2, o ALX_R(3) deve fechar    
											    				
															 	contChaveDeEcontro = contChaveDeEcontro + 1; //Já sei que algum trecho poderá ser suprido %%%%%%%%%%%%%%%%%%%%%%%%%%%
															 	
															 	exibirAviso(myAgent, "A carga disponível é >"+cargaDiponivel+" e a carga total perdida é >"+cargaTotalPerdida+". No caso cargaDisponivel > cargaTotalPerdida");
															 	exibirAviso(myAgent, "A carga disponível é >"+cargaDiponivel+" e a carga total perdida é >"+cargaTotalPerdida+". Houve sobrecarga, mas a carga diponivel "+cargaDiponivel+" > "+cargaTrecho+" carga esta de "+nome);
											    		}else{ //Se mesmo com microrredes não for possível
//											    				cargaTotalPerdida = cargaTotalPerdida - cargaTrecho;
//											    				cargaTotalDisponivelMicrorrede = cargaTotalDisponivelMicrorrede - cargaMicrorredeTrecho; //Como esse trecho terá que sair a microrrede sai tbm										    			}//Ai vai pra próxima iteração
											    				
											    				refFechar = referenciaDaChave + 2; //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//											    				exibirAviso(myAgent, "O valor de carga total perdida foi atualizado (no caso diminuido) e agora é: "+cargaTotalPerdida);
											    				exibirAviso(myAgent, "Deve ser fechada até a chave "+refFechar);
											    		}
														 
										    		}
										    	}// Fim do if para saber se há chave
										    }// Fim do while(i.hasNext())
										   
//										    if(refFechar>0){ %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
										    if(contChaveDeEcontro>0){
											    	 /**********************************************************************************
													     * Protocolo FIPA Request para solicitar somente as chaves a jusante da falta que fechem
													     * e AEs
													     *********************************************************************************/
												  		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
												  		msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
													  		
												  		//Cria-se uma lista para percorrer a tag agentesArmazenamento e ver seus elementos
												  		List lista5 = agenteALBD.getChild("microrredes").getChildren(); 
														Iterator i5 = lista5.iterator();
										    				
													
														
									    				while(i5.hasNext()) { 
													    	Element elemento5 = (Element) i5.next();
													    	String nome5 = String.valueOf(elemento5.getName());
													    	
													    	int refChaveAnalisada = Integer.parseInt(nome5.split("_")[1].split("R")[1]);
													    	
													    	if (nome5!= null && nome5.length()>0 && nome5!= "nenhum"){ //Se houver agentes chave no XML, então add ele como remetente
													    		
//													    		if(refChaveAnalisada>refFechar+1){ //<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
													    		if(refChaveAnalisada>=refFechar){	
													    																    			
													    			msg.addReceiver(new AID((String) nome5, AID.ISLOCALNAME)); //Adicionei a chave
													    		
													    		}
													    	}//Fim do if nome diferente de nada ou nenhum 
									    				}//Fim do segundo while
									    				
									    				
									    				String chaveDeEncontro = agenteALBD.getChild("outrosALs").getChild(ALParticipante).getAttributeValue("chaveDeEncontro"); 	
									    				msg.addReceiver(new AID((String) chaveDeEncontro, AID.ISLOCALNAME)); //Adicionei a chave de encontro
									    				msg.setContent("fechar");
												  		
												  		addBehaviour(new AchieveREInitiator(myAgent, msg) {
															protected void handleInform(ACLMessage inform) {
																System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
															}
															protected void handleRefuse(ACLMessage refuse) {
									//							System.out.println("Agent "+refuse.getSender().getName()+" refused to perform the requested action");
									//							nResponders--;
															}
															protected void handleFailure(ACLMessage failure) {
																if (failure.getSender().equals(myAgent.getAMS())) {
																	// FAILURE notification from the JADE runtime: the receiver
																	// does not exist
																	System.out.println("Responder does not exist");
																}
																else {
																	System.out.println("Agent "+failure.getSender().getName()+" failed to perform the requested action");
																}
															}
															protected void handleAllResultNotifications(Vector notifications) {
									//							if (notifications.size() < nResponders) {
									//								// Some responder didn't reply within the specified timeout
									//								System.out.println("Timeout expired: missing "+(nResponders - notifications.size())+" responses");
									//							}
															}
														}); //Fim do addBehaviour do request initiator
											     }
										     }//Se houverem chaves para serem fechadas
										
									}//Fim do handle Informr
								}); //Fim do contract net initiator
					    	}// Fim do time elapse do waker behaviour
					    });	//Fim do waker behaviour
					   

					} //Fim do if(cont>referenciaDaChaveAtuante) Para saber se tem chaves a jusante do trecho em falta!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1
				    
				    
				    else{ //se não tem chaves a jusante, mas ver se não tem pelo menos microrrede no mesmo trecho.Chegando aqui nada pode ser feito para recomposição. É um alimentador que possui 1 trecho somente
				    	exibirAviso(myAgent, "Não tem chaves a jusante. Vou ver se tem microrredes no trecho afetado que precisem ilhar.");
				    	//*********Saber se tem agente apc e quantos são
						List lista1 = agenteALBD.getChild("microrredes").getChild(agenteChaveSobFalta).getChildren(); 
						Iterator i1 = lista1.iterator();
						
						int contMicrorrede = 0; //inicia cont com zero
						
					    while(i1.hasNext()) { 
					    	Element elemento = (Element) i1.next();
					    	String nome = String.valueOf(elemento.getName());
					    	exibirAviso(myAgent, "Aqui é para aparecer o nome de um agente APC ou 'nenhum'.No caso está aparecendo: "+nome);
					    	
					    	if (nome!= null && nome.length()>0 && nome!="nenhum"){ //Se houver agentes chave no XML, então add ele como remetente
					    		contMicrorrede = contMicrorrede + 1; //Se houver algum agente chave, incrementa o contador
					    	}
					    }
					    exibirAviso(myAgent,"O valor de contMicrorrede é: "+contMicrorrede);
					    
					    if(contMicrorrede>0){ // Se existirem microrredes
//					    if(cont>referenciaDaChaveAtuante){ //Se o número de agente chave for maior que o índice da chave atuante, é porque com certeza há chaves a jusante da chave atuante
							exibirAviso(myAgent, "Há microrredes no trecho afetado. Preciso avisá-las para ilharem.");
					    	/**********************************************************************************
						     * Protocolo FIPA Request para solicitar que APCs abram suas chaves
						     * 
						     *********************************************************************************/
					  		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
					  		msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
					  		msg.setContent("abra");
					  		
					  		
					  		List lista2 = agenteALBD.getChild("microrredes").getChild(agenteChaveSobFalta).getChildren();
							Iterator i2 = lista2.iterator();
							
						    while(i2.hasNext()) { 
						    	Element elemento = (Element) i2.next();
						    	String nome = String.valueOf(elemento.getName());
						    	
								exibirAviso(myAgent, "Analisando se aviso ao agente APC "+nome+" que comande a abertura de seu disjuntor.");
								
						    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){
						    		
				    				exibirAviso(myAgent, "Solicitando ao agente chave "+nome+" que comande a abertura de sua chave.");
//						    				
						    		msg.addReceiver(new AID((String) nome, AID.ISLOCALNAME));
								    		
						    	}// Fim do if para saber se há chave
						    }// Fim do while(i.hasNext())	
					  		
					  		
						    addBehaviour(new AchieveREInitiator(myAgent, msg) {
								protected void handleInform(ACLMessage inform) {
									System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
								}
								protected void handleRefuse(ACLMessage refuse) {
//									System.out.println("Agent "+refuse.getSender().getName()+" refused to perform the requested action");
//									nResponders--;
								}
								protected void handleFailure(ACLMessage failure) {
									if (failure.getSender().equals(myAgent.getAMS())) {
										// FAILURE notification from the JADE runtime: the receiver
										// does not exist
										System.out.println("Responder does not exist");
									}
									else {
										System.out.println("Agent "+failure.getSender().getName()+" failed to perform the requested action");
									}
								}
								protected void handleAllResultNotifications(Vector notifications) {
//									if (notifications.size() < nResponders) {
//										// Some responder didn't reply within the specified timeout
//										System.out.println("Timeout expired: missing "+(nResponders - notifications.size())+" responses");
//									}
								}//fim do protect void handleAllResultNotifications
							}); //Fim do addBehaviour do Request para avisar as micrroredes do trecho onde houve falta
				    
					    }//If cont>0 (se tiver micrroredes no mesmo trecho onde houve falta
//					    else{
////						resposta.setContent("ok");
////						resposta.setPerformative(ACLMessage.AGREE); 
//					    	exibirAviso(myAgent,"Não há microrredes no trecho afetado.");
//					    }
					}//Fim do if tiver chaves a jusante, senão verifica se não tem pelo menos microrrede no trecho afetado
				}// Fim do if(conteudo.equals("curto"))
				else{ //Se não, é porque não houve curto e só está sendo repassado o valor de carga tanto de agentes chave como APCs para atualização
					
					String agenteSender = subscription.getSender().getLocalName();
					String diferenciar = agenteSender.substring(0, 2);
//					exibirAviso(myAgent, "Saber se é agente chave ou microrrede: "+diferenciar);
					
					if(diferenciar.equalsIgnoreCase("AL")){
						//Vou atualizar então esse valor no XML
						String chave = subscription.getSender().getLocalName();
//						exibirAviso(myAgent, "Fui informado de um valor de carga pelo agente chave: "+chave);
						agenteALBD.getChild("chaves").getChild(subscription.getSender().getLocalName()).setAttribute("carga",subscription.getContent());
					}else{//SE não foi chave então é microrrede
						String apc = subscription.getSender().getLocalName();
//						exibirAviso(myAgent, "Fui informado de um valor de carga pelo agente ponto de conexão: "+apc);
						
						String nomeAPC = subscription.getSender().getLocalName(); 
						//A mensagem do apc terá que ser no formato:  "agenteChaveOQualEsseAPCEstáNoMesmoTrecho/potenciaDisponivel/PotenciaBoost"
						String agenteChave = subscription.getContent().split("/")[0]; //agente chave o qual esse microrrede está conectada na mesmo trecho
						String potenciaMicrorrede = subscription.getContent().split("/")[1];  //deltaP da microrrede
						String boost = subscription.getContent().split("/")[2]; //Potencia da Cac que ainda pode ser dado
						
						agenteALBD.getChild("microrredes").getChild(agenteChave).getChild(nomeAPC).setAttribute("potenciaDisponivel",potenciaMicrorrede);
						agenteALBD.getChild("microrredes").getChild(agenteChave).getChild(nomeAPC).setAttribute("boost",boost);
						
						exibirAviso(myAgent, "Recebi uma atualização com o valor de boost igual a >"+boost+" da microrrede do agente >"+apc);
					}
					
				}
				
	
				return resposta;
			}//fim de handleSubscription
			
		});	//Fim do SubscriptionResponder
		
		/********************************************************************************************************
		 * Protocolo FIPA Contract Net para receber solicitações de carga advindos de ALs circunvizinhos
		 ********************************************************************************************************/
		addBehaviour(new ContractNetResponder(this, filtroContractNet) {
			
			private static final long serialVersionUID = 1L;
			
			ArrayList nomesAPCs = new ArrayList();
//			int contUREDE = 0;  //saber se será preciso acionar microrrede para dar um boost
			
			protected ACLMessage handleCfp(ACLMessage cfp) throws NotUnderstoodException, RefuseException {
				/* 1 Saber carga própria (nesse caso sempre será a da primeira chave)
				 * 2 Ver limite da fonte limFonte >= Ipropria + Imultua?
				 * 3 Analisa trecho por trecho para ver se não haverá sobrecarga 
				 */
				
				ACLMessage propose = cfp.createReply();
				propose.setPerformative(ACLMessage.PROPOSE);
				
				//Saber a própria carga. Sempre será a da primeira chave, visto que não está sendo analisado faltas duplas
				String agenteChave1 = getLocalName().concat("_R1");	//saber a carga do primeiro agente chave			
				double cargaPropria = Double.parseDouble(agenteALBD.getChild("chaves").getChild(agenteChave1).getAttributeValue("carga")); //Carga própria do AL participante ou solicitado
				
				//Carga solicitada por outro Alimentador
				double cargaSolicitada = Double.parseDouble(cfp.getContent());   //Carga perdida em outro alimentador em falta
				double cargaDisponivel = 0; //por enquanto a carga disponível é zero. A tendência é rejeitar ajuda num primeiro momento antes de começar a análise
				
				//Analisar se a fonte é capaz de suprir tudo 
				double limFonte = Double.parseDouble(agenteALBD.getChildText("limFonte"));
				
				//Primeiro analisa-se se é possível suprir todo o valor que foi solicitado pelo AL initiator
				if(limFonte >= cargaPropria + cargaSolicitada){ //A fonte é capaz de suprir tudo (sua carga mais a solicitada)?
					exibirAviso(myAgent, "A minha fonte de valor >"+limFonte+" pode suprir a carga solicitada de >"+cargaSolicitada);
//					cargaDisponivel = cargaSolicitada; //Se a fonte puder suprir tudo, então a carga disponível é toda a carga solicitada <<<<<<<<<<<<<<<<######$$$$$$$$$$$$$
//					cargaDisponivel = limFonte - cargaPropria; //<<<<<<<<-------
//					exibirAviso(myAgent, "Minha carga disponível é "+cargaDisponivel);
					
					cargaDisponivel = 0;
					
					List lista11 = agenteALBD.getChild("chaves").getChildren(); 
					Iterator i11 = lista11.iterator();
					
				    while(i11.hasNext()) { 
				    	Element elemento11 = (Element) i11.next();
				    	String nome = String.valueOf(elemento11.getName()); //Nome da chave
				    	
				    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes chave no XML
				    		String referencia = nome.split("_")[1];
				    		
				    		if(referencia.equalsIgnoreCase("R1")){
				    			cargaDisponivel = Double.parseDouble(elemento11.getAttributeValue("capacidade")) - cargaPropria;   //<<<<<<<<-------
				    			exibirAviso(myAgent, "Minha carga disponível inicial é "+cargaDisponivel);
				    		}
				    	}
				    }
					
					//Uma vez que a fonte pode suprir, é Verificar agora se não há sobrecargas em cada trecho do AL solicitado
					List lista = agenteALBD.getChild("chaves").getChildren(); 
					Iterator i = lista.iterator();
					
					int cont = 0;  //Só pra ver se o while analisa os trechos na sequencia certa do trecho mais a jusante R1 para o a montate..
					
				    while(i.hasNext()) { 
				    	Element elemento = (Element) i.next();
				    	String nome = String.valueOf(elemento.getName()); //Nome da chave
				    	
				    	   	
				    	cont = cont+1;
				    	exibirAviso(myAgent, "Analisando sobrecarga no trecho da chave: "+nome+". O valor de cont é: "+cont);
				    	
				    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes chave no XML
					
				    		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//				    		if(cargaSolicitada != cargaDisponivel){ //Se a carga solicitada for diferente da disponível é porque já houve sobrecarga no trecho a montante (iteração anterior) <<<<<<<<
				    		if(cargaSolicitada > cargaDisponivel){	//Se a carga solicitada for maior que a disponível <<<<<<<<<<<<<<<<######$$$$$$$$$$$$$
				    			exibirAviso(myAgent, "Já houve sobrecarga em algum trecho anterior!");
				    			//Se por acaso no trecho anterior houve sobrecarga, vejo se tem microrrede para boost nesse trecho atual para compensar o que o anterior não pode dar
					    		double IdisponivelDaMicrorrede = 0; //Vou analisar se tem microrredes no trecho para poder darem um boost. Se não tiver, então IdisponveilMicrorrede continuará zero
					    		
					    		List lista2 = agenteALBD.getChild("microrredes").getChild(nome).getChildren();
					    		Iterator i2 = lista2.iterator();
					    		
					    		while(i2.hasNext()) {//While só pra saber o boost de todas as microrredes conectadas a esse trecho tendo o agente chave como responsável 
							    	Element elemento2 = (Element) i2.next();
							    	String nome2 = String.valueOf(elemento2.getName()); //Nome da microrrede
//							    	exibirAviso(myAgent, "Vou analisar a microrrede >>>"+nome2+" no trecho >"+nome2);
							    	
							    	if (nome2!= null && nome2.length()>0 && nome2!= "nenhum"){ //Se houver agentes apc no XML
							    		exibirAviso(myAgent, "Analisando se a microrrede "+nome2+" está injetando potência na rede.");
							    		IdisponivelDaMicrorrede = IdisponivelDaMicrorrede + Double.parseDouble(elemento2.getAttributeValue("boost"));
							    		exibirAviso(myAgent, "O valor do boost é: "+elemento2.getAttributeValue("boost"));
							    		
							    		//colocar um array pegando o nome dessa microrrede ################################################################################################
							    		nomesAPCs.add(nome2); //add aqui o nome da microrrede
//							    		contUREDE = contUREDE+1;
							    		exibirAviso(myAgent, "O valor da contribuição de microrredes em >" +nome+" é >"+IdisponivelDaMicrorrede);
							    	}//fim do if para saber se nome2 diferente de nenhum
							    	else{// senão, então não há microrredes 
							    		exibirAviso(myAgent, "Não há microrredes no mesmo trecho monitorado por "+nome);
							    	}
					    		}//Fim do while para percorrer as microrredes
					    		
//					    		exibirAviso(myAgent, "O valor da contribuição de microrredes em >" +nome+" é >"+IdisponivelDaMicrorrede);
					    		
					    		double cargaDoTrecho = Double.parseDouble(elemento.getAttributeValue("carga")); //<<<<<<<----
					    		double capacidadeDoCondutor = Double.parseDouble(elemento.getAttributeValue("capacidade")); //<<<<<----
					    		
					    		exibirAviso(myAgent, "A carga do trecho da chave >"+nome+" é >"+cargaDoTrecho+". A carga solicitada é "+cargaSolicitada+". A capacidade de seu condutor é > "+capacidadeDoCondutor);
//					    		exibirAviso(myAgent, "Somando a carga do trecho atual mais a solicitada, dá um valor de "+(cargaDoTrecho+cargaSolicitada)+". Logo a carga disponivel é "+(capacidadeDoCondutor-cargaDoTrecho-cargaSolicitada));
					    		
					    		if(capacidadeDoCondutor >= cargaDoTrecho + cargaDisponivel + IdisponivelDaMicrorrede){
					    			cargaDisponivel = cargaDisponivel + IdisponivelDaMicrorrede;
					    			//E não altero o valor de carga disponível
					    			exibirAviso(myAgent, "Não há sobrecarga no trecho de "+nome+" com a contribuição de microrrede com valor de >"+IdisponivelDaMicrorrede);
					    		}
					    		else if(capacidadeDoCondutor >= cargaDoTrecho + cargaDisponivel){
					    			//Não altero o valor de carga disponível
					    		}
					    		else{
//					    			cargaDisponivel = (capacidadeDoCondutor - cargaSolicitada)*0.9;
					    			cargaDisponivel = (capacidadeDoCondutor - cargaDoTrecho);
					    			exibirAviso(myAgent, "Há sobrecarga no trecho de "+nome+". O valor de carga disponível passa a ser: "+cargaDisponivel);
					    		}
					    	
				    		}// //Fim do if para saber se já houve sobrecarga
				    		
				    		else{//senão tá diferente do valor de carga perdido e o disponível, então é porque não houve sobrecarga. Vou continuar a análise de sobrecarga normal <<<<<<<<<<<<<<<
					    		if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes chave no XML
					    			exibirAviso(myAgent, "Não houve sobrecarga em algum trecho anterior!");
					    			
					    			double cargaDoTrecho = Double.parseDouble(elemento.getAttributeValue("carga"));   //<<<<<<<<-------
						    		double capacidadeDoCondutor = Double.parseDouble(elemento.getAttributeValue("capacidade"));   //<<<<<<<<-------
						    		
//						    		exibirAviso(myAgent, "A carga do trecho da chave >"+nome+" é >"+cargaDoTrecho+". A capacidade de seu condutor é > "+capacidadeDoCondutor);
						    		exibirAviso(myAgent, "A carga do trecho da chave >"+nome+" é >"+cargaDoTrecho+". A carga solicitada é "+cargaSolicitada+". A capacidade de seu condutor é > "+capacidadeDoCondutor);
//						    		exibirAviso(myAgent, "Somando a carga do trecho atual mais a solicitada, dá um valor de "+(cargaDoTrecho+cargaSolicitada)+". Logo a carga disponivel é "+(capacidadeDoCondutor-cargaDoTrecho-cargaSolicitada));
						    		
//						    		if(capacidadeDoCondutor >= cargaDoTrecho + cargaDisponivel){
						    		if(capacidadeDoCondutor >= cargaDoTrecho + cargaSolicitada){
						    			exibirAviso(myAgent, "Não há sobrecarga no trecho de "+nome);
//						    			cargaDisponivel = capacidadeDoCondutor-cargaDoTrecho-cargaSolicitada; //<<
						    			//E não altero o valor de carga disponível
						    		}
						    		else{
//						    			cargaDisponivel = (capacidadeDoCondutor - cargaSolicitada)*0.9;
						    			cargaDisponivel = (capacidadeDoCondutor - cargaDoTrecho);
						    			exibirAviso(myAgent, "Há sobrecarga no trecho de "+nome+". O valor de carga disponível passa a ser: "+cargaDisponivel);
						    		}
						    		
						    	}// Fim do if (nome!= null && nome.length()>0 && nome!= "nenhum")
					    	
					    	}//Fim do else pra saber se houve sobrecarga ou não <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<</////////////////////////////////////////
				    		
				    	}//Fim do if para saber se nome diferente de "nenhum"  <<<<<<<<<<<<<<<<<<<<<,
				    	
				    	
				    }// Fim do while
					
				   
//				    ACLMessage propose = cfp.createReply();
//					propose.setPerformative(ACLMessage.PROPOSE);
					propose.setContent(String.valueOf(cargaDisponivel));
//					return propose;
				}// Fim do if(limFonte >= cargaPropria + cargaSolicitada)
				else{ //Se não puder suprir tudo, ele deverá suprir pelo menos uma parte. AQUI AINDA É EM TERMOS DE FONTE E NÃO DE TRECHO
					exibirAviso(myAgent, "Não posso suprir todo o valor solicitado que é de >"+cargaSolicitada+", mas posso suprir >");
					
//					cargaSolicitada = (limFonte - cargaPropria)*0.9; //A carga solicitada passa a ser só o que o AL é capaz de suprir
					cargaSolicitada = (limFonte - cargaPropria); //A carga solicitada passa a ser só o que o AL é capaz de suprir
					
					//Verificar agora se não há sobrecargas em cada trecho com o valor de carga disponível recalculado
					List lista = agenteALBD.getChild("chaves").getChildren(); 
					Iterator i = lista.iterator();
					
				    while(i.hasNext()) { 
				    	Element elemento = (Element) i.next();
				    	String nome = String.valueOf(elemento.getName());
				    	
				    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){ //Se houver agentes chave no XML
				    		double cargaDoTrecho = Double.parseDouble(elemento.getAttributeValue("carga"));
				    		double capacidadeDoCondutor = Double.parseDouble(elemento.getAttributeValue("capacidade"));
				    		
				    		if(capacidadeDoCondutor >= cargaDoTrecho + cargaDisponivel){
				    			exibirAviso(myAgent, "Não há sobrecarga no meu trecho. ");
				    		}
				    		else{
				    			
//				    			cargaDisponivel = (capacidadeDoCondutor - cargaSolicitada)*0.9;
				    			cargaDisponivel = (capacidadeDoCondutor - cargaDoTrecho);
				    			exibirAviso(myAgent, "Há sobrecarga no trecho de "+nome+". O valor de carga disponível passa a ser: "+cargaDisponivel);
				    		}
				    		
				    	}// Fim do if (nome!= null && nome.length()>0 && nome!= "nenhum")
				    }// Fim do while
				    
//				    ACLMessage propose = cfp.createReply();
//					propose.setPerformative(ACLMessage.PROPOSE);
					propose.setContent(String.valueOf(cargaDisponivel));
//					return propose;
				}//Fim da análise de capacidade da fonte, se dá pra suprir tudo ou só uma parte
				
//				ACLMessage propose = cfp.createReply();
//				propose.setPerformative(ACLMessage.PROPOSE);
//				propose.setContent(String.valueOf(cargaDisponivel));
				return propose;
//				
			}//Fim do handle cfp

			protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose,ACLMessage accept) throws FailureException {
					
					int quantUREDE = nomesAPCs.size();
//					if(contUREDE>0){ //preciso solicitar microrrede(s) que dêem um boost acionando geração controlada
					if(quantUREDE>0){	
						exibirAviso(myAgent, "Há microrredes pra serem solicitadas suas fontes controladas. Ao todo: "+quantUREDE);
						/**********************************************************************************
					     * Protocolo FIPA Request Initiator para solicitar microrrede acionem suas gerações controladas
					     * 
					     *********************************************************************************/
				  		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
				  		msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
					  		
				  		for (int i=0; i<quantUREDE; i++) {
				  			exibirAviso(myAgent,"Adicionando a microrrede >"+nomesAPCs.get(i)+" na mensagem request para solicitar que use a CaC");
				  	      msg.addReceiver(new AID((String) nomesAPCs.get(i), AID.ISLOCALNAME));
				  		}
				  		
					    msg.setContent("boost");
				  		
				  		addBehaviour(new AchieveREInitiator(myAgent, msg) {
							protected void handleInform(ACLMessage inform) {
//								System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
							}
							protected void handleRefuse(ACLMessage refuse) {
	//							System.out.println("Agent "+refuse.getSender().getName()+" refused to perform the requested action");
	//							nResponders--;
							}
							protected void handleFailure(ACLMessage failure) {
								if (failure.getSender().equals(myAgent.getAMS())) {
									// FAILURE notification from the JADE runtime: the receiver
									// does not exist
//									System.out.println("Responder does not exist");
								}
								else {
//									System.out.println("Agent "+failure.getSender().getName()+" failed to perform the requested action");
								}
							}
							protected void handleAllResultNotifications(Vector notifications) {
	//							if (notifications.size() < nResponders) {
	//								// Some responder didn't reply within the specified timeout
	//								System.out.println("Timeout expired: missing "+(nResponders - notifications.size())+" responses");
	//							}
							}
						}); //Fim do addBehaviour do request initiator
											
					}//Fim do if(quantUREDES>0)
					
					//Aqui respondo normalmente ao AL initiator
					ACLMessage inform = accept.createReply();
					inform.setPerformative(ACLMessage.INFORM);
					inform.setContent(accept.getContent());
			
					return inform;
			}

			protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
				System.out.println("Agent "+getLocalName()+": Proposal rejected");
				
				
			}
		} ); //Fim do comportamento contract net
	} // fim do public void setup
		

	public void exibirMensagem(ACLMessage msg){
		
		System.out.println("\n\n===============<<MENSAGEM>>==============");
		System.out.println("De: " + msg.getSender());
		System.out.println("Para: " + this.getName());
		System.out.println("Conteudo: " + msg.getContent());
		System.out.println("=============================================");
	}
	
	public void exibirAviso(Agent myAgent, String aviso){
    	
		System.out.println("\n\n-----------------<<AVISO>>------------------");
		System.out.println("Agente: "+myAgent.getLocalName());
		System.out.println("Aviso: " +aviso);
		
		Calendar cal = Calendar.getInstance();
    	cal.getTime();
//		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
//    	SimpleDateFormat sdf = new SimpleDateFormat("E yyyy.MM.dd 'at' hh:mm:ssss a zzz");
    	SimpleDateFormat sdf = new SimpleDateFormat("E dd.MM.yyyy 'at' hh:mm:ssss a");
    	System.out.println( sdf.format(cal.getTime()) );
    	
//		System.out.println(System . currentTimeMillis ());
	}

	/**
	 *  Método para carregamento do XML
	 * 
	 */
	public Element carregaBD(String nomeAgente) {
	
		// Declaração de variáveis, uma de cada tipo
		File endereco = null; // Desnecessário se usarmos o endereço do arq.
								// dentro do doc dentro do try
		SAXBuilder builder = null; // usado p/ processar a estrut. do doc. p/
									// dentro da variável do tipo documento
		Document doc = null;
		Element BD = null;
	
		endereco = new File("src/XML/" + nomeAgente + ".xml");
	
		builder = new SAXBuilder();
	
		try { // Criando o arquivo propriamente dito
			doc = builder.build(endereco);
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}// fim do try
	
		BD = doc.getRootElement();
		return BD;
	
	}// fim do método carregarBD
	
}
