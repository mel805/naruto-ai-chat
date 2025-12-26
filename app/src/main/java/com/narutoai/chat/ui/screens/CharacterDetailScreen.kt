package com.narutoai.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.narutoai.chat.models.Character

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailScreen(
    character: Character,
    onBackClick: () -> Unit,
    onStartChat: () -> Unit,
    onGenerateGallery: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Profil", "Histoire", "Galerie")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(character.name) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = onGenerateGallery) {
                        Icon(Icons.Default.PhotoLibrary, "Générer galerie")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartChat,
                icon = { Icon(Icons.Default.Chat, "Chat") },
                text = { Text("Démarrer la conversation") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Hero Section avec image principale
            HeroSection(character)
            
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Content selon tab sélectionné
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Profil - Description physique et personnalité
                        item { ProfileTab(character) }
                    }
                    1 -> {
                        // Histoire - Scénario et background
                        item { StoryTab(character) }
                    }
                    2 -> {
                        // Galerie photos
                        item { GalleryTab(character, onGenerateGallery) }
                    }
                }
            }
        }
    }
}

@Composable
fun HeroSection(character: Character) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        // Image de fond
        AsyncImage(
            model = if (character.thumbnailUrl.isNotEmpty()) 
                character.thumbnailUrl 
            else 
                character.imageResId,
            contentDescription = character.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Gradient overlay pour lisibilité
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )
        
        // Info en bas
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = character.avatarEmoji,
                fontSize = 48.sp
            )
            Text(
                text = character.name,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = character.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun ProfileTab(character: Character) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Informations physiques
        if (character.physicalDescription.isNotEmpty()) {
            SectionCard(
                title = "Description Physique",
                icon = Icons.Default.Person
            ) {
                Text(
                    text = character.physicalDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Stats physiques
                if (character.age.isNotEmpty()) {
                    InfoChip("👤 Âge", character.age)
                }
                if (character.height.isNotEmpty()) {
                    InfoChip("📏 Taille", character.height)
                }
                if (character.hairColor.isNotEmpty()) {
                    InfoChip("💇 Cheveux", character.hairColor)
                }
                if (character.eyeColor.isNotEmpty()) {
                    InfoChip("👁️ Yeux", character.eyeColor)
                }
                if (character.bodyType.isNotEmpty()) {
                    InfoChip("💪 Physique", character.bodyType)
                }
            }
        }
        
        // Traits distinctifs
        if (character.distinctiveFeatures.isNotEmpty()) {
            SectionCard(
                title = "Traits Distinctifs",
                icon = Icons.Default.Star
            ) {
                character.distinctiveFeatures.forEach { feature ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("•", fontWeight = FontWeight.Bold)
                        Text(feature, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        
        // Personnalité et tempérament
        if (character.temperament.isNotEmpty()) {
            SectionCard(
                title = "Tempérament",
                icon = Icons.Default.Mood
            ) {
                Text(
                    text = character.temperament,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Traits de caractère
        if (character.characterTraits.isNotEmpty()) {
            SectionCard(
                title = "Caractère",
                icon = Icons.Default.Psychology
            ) {
                character.characterTraits.forEach { trait ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(trait, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        
        // Likes & Dislikes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (character.likes.isNotEmpty()) {
                SectionCard(
                    title = "Aime",
                    icon = Icons.Default.Favorite,
                    modifier = Modifier.weight(1f)
                ) {
                    character.likes.forEach { like ->
                        Text(
                            text = "💚 $like",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
            
            if (character.dislikes.isNotEmpty()) {
                SectionCard(
                    title = "N'aime pas",
                    icon = Icons.Default.Close,
                    modifier = Modifier.weight(1f)
                ) {
                    character.dislikes.forEach { dislike ->
                        Text(
                            text = "💔 $dislike",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
        
        // Compétences
        if (character.skills.isNotEmpty()) {
            SectionCard(
                title = "Compétences",
                icon = Icons.Default.EmojiEvents
            ) {
                character.skills.forEach { skill ->
                    Surface(
                        modifier = Modifier.padding(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = skill,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StoryTab(character: Character) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Scénario principal
        if (character.scenario.isNotEmpty()) {
            SectionCard(
                title = "Scénario",
                icon = Icons.Default.Book
            ) {
                Text(
                    text = character.scenario,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 24.sp
                )
            }
        }
        
        // Histoire complète
        if (character.backgroundStory.isNotEmpty()) {
            SectionCard(
                title = "Histoire Complète",
                icon = Icons.Default.AutoStories
            ) {
                Text(
                    text = character.backgroundStory,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 24.sp
                )
            }
        }
        
        // Citation favorite (si disponible)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.FormatQuote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = getCharacterQuote(character),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
fun GalleryTab(character: Character, onGenerateGallery: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Message si galerie vide
        if (character.gallery.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Aucune image dans la galerie",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Générez des images hyper-réalistes de ${character.name} avec Pollination AI",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Button(
                        onClick = onGenerateGallery,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Générer la galerie")
                    }
                }
            }
        } else {
            // Grille d'images
            Text(
                text = "Galerie (${character.gallery.size} photos)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Afficher les images en grille 2 colonnes
            character.gallery.chunked(2).forEach { rowImages ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowImages.forEach { imageUrl ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(3f / 4f),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = character.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    // Remplir l'espace si nombre impair
                    if (rowImages.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            content()
        }
    }
}

@Composable
fun InfoChip(label: String, value: String) {
    Row(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

fun getCharacterQuote(character: Character): String {
    return when (character.id) {
        "naruto" -> "\"Je ne reviens jamais sur ma parole, c'est ma voie ninja!\" - Naruto"
        "sasuke" -> "\"Je vais restaurer mon clan et tuer un certain homme...\" - Sasuke"
        "hinata" -> "\"Je voulais marcher à tes côtés... toujours...\" - Hinata"
        "sakura" -> "\"Je ne vais plus être un fardeau pour vous!\" - Sakura"
        "kakashi" -> "\"Dans le monde ninja, ceux qui brisent les règles sont des rebuts... mais ceux qui abandonnent leurs amis sont pires que des rebuts!\" - Kakashi"
        "itachi" -> "\"Peu importe quelle puissance tu obtiens, je serais toujours là, comme mur devant toi.\" - Itachi"
        else -> "\"${character.description}\""
    }
}
